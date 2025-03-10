package top.fifthlight.stubgen

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import kotlin.concurrent.withLock
import kotlin.io.path.outputStream

object StubGen {
    fun run(inputs: List<InputEntry>, outputPath: Path) {
        val classMap = ClassMap()
        inputs.parallelStream()
            .forEach { input ->
                classMap.visitCount.incrementAndGet()
                for (jarInput in input.jars) {
                    val mapping = jarInput.mapping?.let(::readProguardMapping)
                    val remapper = mapping?.let(::ProguardRemapper)
                    val inputFile = JarFile(jarInput.path.toFile())
                    for (entry in inputFile.entries()) {
                        if (entry.name.equals("module-info.class", ignoreCase = true)) {
                            continue
                        }
                        if (entry.name.equals("package-info.class", ignoreCase = true)) {
                            continue
                        }
                        if (!entry.name.endsWith(".class")) {
                            continue
                        }

                        var output: ClassVisitor = StubGenClassVisitor(classMap)
                        if (remapper != null) {
                            output = ClassRemapper(output, remapper)
                        }
                        ClassReader(inputFile.getInputStream(entry)).accept(
                            output,
                            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                        )
                    }
                }
            }

        // Do final clean and write output
        classMap.cleanUnvisitedItems()
        JarOutputStream(outputPath.outputStream()).use { outputStream ->
            val outputLock = ReentrantLock()
            classMap.classes.entries.parallelStream()
                .map { (name, clazz) ->
                    val writer = ClassWriter(Opcodes.ASM9)
                    clazz.write(
                        visitor = writer,
                        classVersion = clazz.info.version,
                        removedClasses = classMap.removedClasses,
                    )
                    Pair(name, writer.toByteArray())
                }
                .forEach { (name, clazz) ->
                    outputLock.withLock {
                        outputStream.putNextEntry(ZipEntry("$name.class"))
                        outputStream.write(clazz)
                    }
                }
        }
    }
}
