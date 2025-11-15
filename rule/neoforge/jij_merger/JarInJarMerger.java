package top.fifthlight.armorstand.jijmerger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarInJarMerger {
    private record JijMetadata(@JsonProperty("jars") List<Jar> jars) {
        public record Jar(@JsonProperty("identifier") Identifier identifier, @JsonProperty("version") Version version,
                          @JsonProperty("path") String path, @JsonProperty("isObfuscated") Boolean isObfuscated) {
            public record Identifier(@JsonProperty("group") String group, @JsonProperty("artifact") String artifact) {
            }

            public record Version(@JsonProperty("range") String range,
                                  @JsonProperty("artifact") String artifactVersion) {
            }
        }
    }

    private record JijEntry(Path path, String group, String artifact, String version, String fmlType) {
    }

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("([^:]+):([^:]+):([^:]+):([^:]*)");
    private static final ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

    public static void main(String[] args) throws IOException {
        var inputJar = Path.of(args[0]);
        var outputJar = Path.of(args[1]);
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Bad arguments length: " + args.length);
        }

        var jarEntries = new ArrayList<JijEntry>();
        for (var i = 2; i < args.length; i += 2) {
            var descriptionStr = args[i];
            var filePath = Path.of(args[i + 1]);
            var matcher = DESCRIPTION_PATTERN.matcher(descriptionStr);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Bad description: " + descriptionStr);
            }
            var description = new JijEntry(filePath, matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
            jarEntries.add(description);
        }

        try (var inputStream = new ZipInputStream(Files.newInputStream(inputJar));
             var outputStream = new ZipOutputStream(Files.newOutputStream(outputJar))) {
            var jijJarEntries = new ArrayList<JijMetadata.Jar>();
            for (var jijEntry : jarEntries) {
                var path = "META-INF/jarjar/" + jijEntry.group + "_" + jijEntry.artifact + "_" + jijEntry.version + ".jar";
                try (var entryByteOutputStream = new ByteArrayOutputStream();
                     var entryInputStream = new JarInputStream(Files.newInputStream(jijEntry.path()))) {
                    var manifest = entryInputStream.getManifest();
                    if (!jijEntry.fmlType.isBlank()) {
                        manifest.getMainAttributes().putValue("FMLType", jijEntry.fmlType);
                    }
                    try (var entryOutputStream = new ZipOutputStream(entryByteOutputStream)) {
                        var entry = new JarEntry("META-INF/MANIFEST.MF");
                        entry.setTime(0L);
                        entryOutputStream.putNextEntry(entry);
                        manifest.write(entryOutputStream);
                        entryOutputStream.closeEntry();

                        while ((entry = entryInputStream.getNextJarEntry()) != null) {
                            entry.setTime(0L);
                            entryOutputStream.putNextEntry(entry);
                            entryInputStream.transferTo(entryOutputStream);
                            entryOutputStream.closeEntry();
                        }
                    }
                    var entry = new JarEntry(path);
                    entry.setTime(0L);
                    outputStream.putNextEntry(entry);
                    entryByteOutputStream.writeTo(outputStream);
                    outputStream.closeEntry();
                }
                jijJarEntries.add(new JijMetadata.Jar(new JijMetadata.Jar.Identifier(jijEntry.group, jijEntry.artifact),
                        new JijMetadata.Jar.Version("[" + jijEntry.version + ",)", jijEntry.version), path, false));
            }
            var jijMetadata = new JijMetadata(jijJarEntries);
            var jijMetadataEntry = new JarEntry("META-INF/jarjar/metadata.json");
            jijMetadataEntry.setTime(0L);
            outputStream.putNextEntry(jijMetadataEntry);
            mapper.writeValue(outputStream, jijMetadata);
            outputStream.closeEntry();

            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                entry.setTime(0L);
                outputStream.putNextEntry(entry);
                inputStream.transferTo(outputStream);
                outputStream.closeEntry();
            }
        }
    }
}
