import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.nio.file.Path
import kotlin.io.path.relativeTo

group = "top.fifthlight.touchcontroller"
version = "0.0.1"

val localProperties: Map<String, String> by rootProject.ext

val targets = mapOf(
    "i386" to "i386-linux-gnu",
    "x86_64" to "x86_64-linux-gnu",
    "arm" to "arm-linux-gnueabihf",
    "aarch64" to "aarch64-linux-gnu",
)

val imageName: String = localProperties["image.linux-gnu"] ?: "localhost/touchcontroller-linux:latest"

// Example configuration for toolbox in Fedora Sliverblue:
// podman.command=podman-remote
// podman.extra-args=--security-opt label=disable
val podmanCommand: String = localProperties["podman.command"] ?: "podman"
val podmanExtraArguments = localProperties["podman.extra-args"]?.split(" ") ?: listOf()

val rootProjectAbsolutePath: Path = rootProject.projectDir.toPath().toAbsolutePath()
val projectAbsolutePath: Path = projectDir.toPath().toAbsolutePath()
val projectRelativePath = projectAbsolutePath.relativeTo(rootProjectAbsolutePath)

val compileNativeTasks = targets.mapValues { (arch, target) ->
    task<Exec>("compileNative${arch.uppercaseFirstChar()}") {
        val buildCommands = listOf(
            "cmake -DCMAKE_TOOLCHAIN_FILE=/toolchain/$target.cmake -S . -B build/cmake/$arch",
            "cmake --build build/cmake/$arch --config Release --parallel ${Runtime.getRuntime().availableProcessors()}",
        ).joinToString("; ")
        commandLine(buildList {
            add(podmanCommand)
            add("run")
            add("--rm")
            addAll(podmanExtraArguments)
            add("-w")
            add("/work/$projectRelativePath")
            add("-v")
            add("$rootProjectAbsolutePath:/work")
            add(imageName)
            add("bash")
            add("-ec")
            add(buildCommands)
        })
        inputs.apply {
            property("image.linux-gnu", imageName)
            property("podman.command", podmanCommand)
            property("podman.extra-args", podmanExtraArguments)
            files("CMakeLists.txt")
            dir("src")
        }
        outputs.file("build/cmake/$arch/libproxy_linux_wayland.so")
    }
}

val compileNativeTask = task("compileNative") {
    dependsOn(compileNativeTasks)
}

val compileTask = task("compile") {
    dependsOn(compileNativeTask)
}

val assembleTask = task<Jar>("assemble") {
    archiveFileName = "TouchController-Proxy-Linux.jar"
    destinationDirectory = layout.buildDirectory.dir("lib")
    targets.forEach { (arch, target) ->
        from(compileNativeTasks[arch]!!.outputs) {
            into(target)
        }
    }
    dependsOn(compileTask)
}

task<Delete>("clean") {
    delete(layout.buildDirectory)
}

task("build") {
    dependsOn(assembleTask)
}

configurations {
    register("default")
}

artifacts {
    add("default", assembleTask)
}
