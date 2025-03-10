package top.fifthlight.touchcontoller.gradle.task

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import top.fifthlight.stubgen.InputEntry
import top.fifthlight.stubgen.InputRemapper
import top.fifthlight.stubgen.JarInput
import top.fifthlight.stubgen.StubGen
import top.fifthlight.touchcontoller.gradle.data.Library
import top.fifthlight.touchcontoller.gradle.data.VersionInfo
import top.fifthlight.touchcontoller.gradle.data.VersionManifest
import top.fifthlight.touchcontoller.gradle.ext.toHexString
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.stream.Collectors
import kotlin.io.path.*

abstract class GenerateStubTask : DefaultTask() {
    @get:Input
    abstract val versions: ListProperty<String>

    @get:Input
    abstract val manifestUrl: Property<URI>

    @get:OutputDirectory
    abstract val libraryDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private val cacheDir by lazy {
        project.rootProject.layout.projectDirectory.dir(".gradle/minecraft-stub").asFile.toPath()
    }

    private fun cacheDir(path: String): Path = cacheDir.resolve(path).apply { createDirectories() }

    private fun getLibraryPath(basePath: Path, library: Library): Path {
        val namePartLists = library.name.split(':').toMutableList()
        namePartLists[namePartLists.lastIndex] = "${namePartLists[namePartLists.lastIndex]}.jar"
        val nameParts = namePartLists.subList(1, namePartLists.size).toTypedArray()
        val name = Path.of(namePartLists[0], *nameParts)
        return basePath.resolve(name)
    }

    @TaskAction
    fun generate() {
        outputFile.get().asFile.let { file ->
            if (file.exists()) {
                println("Output file $file exists, skip generating stubs.")
                return
            }
        }
        cacheDir.createDirectories()
        val manifest = downloadVersionManifest()
        val pendingVersions = versions.get()

        val versionManifests = pendingVersions
            .parallelStream()
            .map { version ->
                Pair(version, getVersionInfo(manifest, version))
            }
            .collect(
                Collectors.toMap(
                    { (version, _) -> version },
                    { (_, info) -> info }
                )
            )

        val libraryPath = libraryDir.get().asFile.toPath()
        val libraryPaths = versionManifests
            .values
            .flatMap { it.libraries }
            .filterNot { it.haveRules }
            .associate { library -> Pair(library.downloadArtifact.sha1, library) }
            .mapValues { (sha1, library) ->
                downloadWithCache(
                    library.downloadArtifact.url,
                    "libraries",
                    "${library.name}.jar",
                    sha1,
                )
            }

        val versionJars = versionManifests.mapValues { (version, manifest) ->
            val clientJar = downloadWithCache(manifest.downloads.client.url, "client-jar", "${manifest.id}.jar")
            val mappingsUrl = manifest.downloads.clientMappings?.url
                ?: throw GradleException("No client mapping for version $version")
            val clientMapping = downloadWithCache(mappingsUrl, "client-mapping", "${manifest.id}.txt")
            JarInput(clientJar, clientMapping)
        }

        val outputPath = outputFile.get().asFile.toPath()
        if (versionJars.size == 1) {
            val version = versionManifests.values.first()
            val versionJar = versionJars.values.first()
            InputRemapper.remap(versionJar, outputPath)
            for (library in version.libraries) {
                val path = libraryPaths[library.downloadArtifact.sha1] ?: continue
                val destPath = getLibraryPath(libraryPath, library)
                destPath.parent.createDirectories()
                try {
                    if (!destPath.exists()) {
                        Files.createSymbolicLink(destPath, path)
                    }
                } catch (_: IOException) {
                    Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
            return
        }

        val commonLibraries = versionManifests.values
            .map { it.libraries.filterNot { it.haveRules }.toSet() }
            .reduce { source, target -> source.intersect(target) }

        val inputEntries = versionManifests.map { (version, manifest) ->
            val versionJar = versionJars[version]!!
            val libraries = manifest.libraries
                .filterNot { it.haveRules }
                .subtract(commonLibraries)
                .map {
                    val sha1 = it.downloadArtifact.sha1
                    val path = libraryPaths[sha1]!!
                    JarInput(path)
                }
            InputEntry(libraries + versionJar)
        }

        generateStub(inputEntries, outputPath)
        for (library in commonLibraries) {
            val sha1 = library.downloadArtifact.sha1
            val path = libraryPaths[sha1]!!
            val destPath = getLibraryPath(libraryPath, library)
            destPath.parent.createDirectories()
            try {
                if (!destPath.exists()) {
                    Files.createSymbolicLink(destPath, path)
                }
            } catch (_: IOException) {
                Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun downloadVersionManifest(): VersionManifest {
        val file = downloadWithCache(manifestUrl.get(), "manifests", "version_manifest.json")
        @Suppress("UNCHECKED_CAST")
        return VersionManifest(JsonSlurper().parse(file) as Map<String, String>)
    }

    private fun getVersionInfo(manifest: VersionManifest, version: String): VersionInfo {
        val entry =
            manifest.versions.firstOrNull { it.id == version } ?: throw GradleException("Version $version not found")
        val file = downloadWithCache(URI(entry.url), "versions", "${entry.id}.json")
        @Suppress("UNCHECKED_CAST")
        return VersionInfo(JsonSlurper().parse(file) as Map<String, String>)
    }

    private fun generateStub(jarFiles: List<InputEntry>, output: Path) {
        output.parent.createDirectories()
        StubGen.run(jarFiles, output)
    }

    private fun checkSha1(file: Path, sha1: String): Boolean = file.inputStream().use { input ->
        MessageDigest.getInstance("SHA-1")
            .digest(input.readBytes())
            .toHexString() == sha1
    }

    private fun downloadWithCache(uri: URI, outputDir: String, outputName: String, checksum: String? = null): Path {
        var cacheDir = cacheDir(outputDir)
        if (checksum != null) {
            cacheDir = cacheDir.resolve(checksum)
        }
        cacheDir.createDirectories()
        val cacheFile = cacheDir.resolve(outputName)

        if (cacheFile.exists()) {
            return cacheFile
        }

        logger.info("Downloading $uri")
        val downloadTemp = cacheFile.parent.resolve(cacheFile.name + "-cache")
        uri.toURL().openStream().use { input ->
            cacheFile.parent.createDirectories()
            downloadTemp.outputStream().use { output ->
                input.transferTo(output)
            }
            downloadTemp.moveTo(cacheFile)
        }

        if (checksum != null && !checkSha1(cacheFile, checksum)) {
            throw GradleException("SHA1 mismatch for file $cacheFile($uri): $checksum")
        }

        return cacheFile
    }
}