package top.fifthlight.fabazel.jarmerger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

// Pool man's shadow
public class JarMerger {
    public static void main(String[] args) throws IOException {
        var outputPath = Path.of(args[0]);
        var processedEntryNames = new HashSet<String>();
        try (var outputStream = new JarOutputStream(Files.newOutputStream(outputPath))) {
            String currentStrip = null;
            for (var i = 1; i < args.length; i++) {
                var arg = args[i];
                switch (arg) {
                    case "--strip" -> currentStrip = args[++i];
                    case "--resource" -> {
                        var filePath = args[++i];
                        var entryPath = filePath;
                        if (currentStrip != null) {
                            if (!entryPath.startsWith(currentStrip)) {
                                throw new IllegalArgumentException("Invalid resource path: " + arg + ", not matching strip: " + currentStrip);
                            }
                            entryPath = entryPath.substring(currentStrip.length());
                            entryPath = entryPath.replace('\\', '/');
                            if (entryPath.startsWith("/")) {
                                entryPath = entryPath.substring(1);
                            }
                        }
                        if (processedEntryNames.contains(entryPath)) {
                            continue;
                        }
                        var entry = new JarEntry(entryPath);
                        entry.setCreationTime(FileTime.fromMillis(0));
                        entry.setLastAccessTime(FileTime.fromMillis(0));
                        entry.setLastModifiedTime(FileTime.fromMillis(0));
                        outputStream.putNextEntry(entry);
                        try (var inputStream = Files.newInputStream(Path.of(filePath))) {
                            inputStream.transferTo(outputStream);
                        }
                        processedEntryNames.add(entryPath);
                    }
                    default -> {
                        var inputPath = Path.of(args[i]);
                        try (var inputStream = new JarInputStream(Files.newInputStream(inputPath))) {
                            JarEntry entry;
                            while ((entry = inputStream.getNextJarEntry()) != null) {
                                if (processedEntryNames.contains(entry.getName())) {
                                    continue;
                                }
                                processedEntryNames.add(entry.getName());
                                entry.setCreationTime(FileTime.fromMillis(0));
                                entry.setLastAccessTime(FileTime.fromMillis(0));
                                entry.setLastModifiedTime(FileTime.fromMillis(0));
                                outputStream.putNextEntry(entry);
                                inputStream.transferTo(outputStream);
                            }
                        }
                    }
                }
            }
        }
    }
}
