package top.fifthlight.fabazel.accesswidenertransformer;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class AccessWidenerTransformer {
    public static void main(String[] args) throws IOException {
        var inputFile = Path.of(args[0]);
        var outputFile = Path.of(args[1]);

        var accessWidener = new AccessWidener();
        var accessWidenerReader = new AccessWidenerReader(accessWidener);
        for (var i = 2; i < args.length; i++) {
            var srcFile = Path.of(args[i]);
            try (var reader = Files.newBufferedReader(srcFile)) {
                accessWidenerReader.read(reader);
            }
        }

        try (var input = new JarInputStream(Files.newInputStream(inputFile)); var output = new JarOutputStream(Files.newOutputStream(outputFile))) {
            JarEntry entry;
            while ((entry = input.getNextJarEntry()) != null) {
                var newEntry = new JarEntry(entry.getName());
                newEntry.setCreationTime(FileTime.fromMillis(0));
                newEntry.setLastAccessTime(FileTime.fromMillis(0));
                newEntry.setLastModifiedTime(FileTime.fromMillis(0));
                output.putNextEntry(newEntry);

                if (entry.getName().endsWith(".class")) {
                    var classReader = new ClassReader(input);
                    var classWriter = new ClassWriter(0);
                    var classVisitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, classWriter, accessWidener);
                    classReader.accept(classVisitor, 0);
                    output.write(classWriter.toByteArray());
                } else {
                    input.transferTo(output);
                }

                input.closeEntry();
                output.closeEntry();
            }
        }
    }
}