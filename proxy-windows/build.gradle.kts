import org.gradle.kotlin.dsl.support.uppercaseFirstChar

group = "top.fifthlight.touchcontroller"
version = "0.0.1"

val localProperties: Map<String, String> by rootProject.ext

val targets = mapOf(
    "i686" to "i686-w64-mingw32",
    "x86_64" to "x86_64-w64-mingw32",
    "aarch64" to "aarch64-w64-mingw32",
)

val imageName: String = localProperties["image.llvm-mingw-jdk"] ?: "localhost/llvm-mingw-jdk"

// Example configuration for toolbox in Fedora Sliverblue:
// podman.command=podman-remote
// podman.extra-args=--security-opt label=disable
val podmanCommand: String = localProperties["podman.command"] ?: "podman"
val podmanExtraArguments = localProperties["podman.extra-args"]?.split(" ") ?: listOf()

val compileNativeTasks = targets.mapValues { (arch, target) ->
    task<Exec>("compileNative${arch.uppercaseFirstChar()}") {
        val buildCommands = listOf(
            "cd /work",
            "JAVA_HOME=lib/jvm cmake -DCMAKE_TOOLCHAIN_FILE=/toolchain/$target.cmake -S . -B build/cmake/$arch",
            "cmake --build build/cmake/$arch -j",
        ).joinToString("; ")
        commandLine(buildList {
            add(podmanCommand)
            add("run")
            add("--rm")
            addAll(podmanExtraArguments)
            add("-v")
            add("${projectDir.absolutePath}:/work")
            add(imageName)
            add("bash")
            add("-ec")
            add(buildCommands)
        })
        inputs.apply {
            property("image.llvm-mingw-jdk", imageName)
            files("CMakeLists.txt")
            dir("src")
        }
        outputs.file("build/cmake/$arch/libproxy_windows.dll")
    }
}

val compileNativeTask = task("compileNative") {
    dependsOn(compileNativeTasks)
}

val compileTask = task("compile") {
    dependsOn(compileNativeTask)
}

val assembleTask = task<Jar>("assemble") {
    archiveFileName = "TouchController-Proxy-Windows.jar"
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
