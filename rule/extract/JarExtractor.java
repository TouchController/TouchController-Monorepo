package top.fifthlight.fabazel.jarextractor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarExtractor {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: <input_jar> <entry_path> <output_file>");
        }

        Path jarPath = Paths.get(args[0]);
        String entryPath = args[1];
        Path outputPath = Paths.get(args[2]);

        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directories", e);
        }

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(entryPath);
            if (entry == null) {
                throw new RuntimeException("Entry '" + entryPath + "' not found in JAR");
            }

            try (InputStream input = jar.getInputStream(entry);
                 OutputStream output = Files.newOutputStream(outputPath)) {
                input.transferTo(output);
            }
        }
    }
}
