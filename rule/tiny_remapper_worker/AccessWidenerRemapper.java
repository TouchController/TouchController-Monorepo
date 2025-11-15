package top.fifthlight.fabazel.remapper;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class AccessWidenerRemapper implements OutputConsumerPath.ResourceRemapper {
    private final Remapper remapper;
    private final String fromNamespace;
    private final String toNamespace;

    public AccessWidenerRemapper(Remapper remapper, String fromNamespace, String toNamespace) {
        this.remapper = remapper;
        this.fromNamespace = fromNamespace;
        this.toNamespace = toNamespace;
    }

    @Override
    public boolean canTransform(TinyRemapper remapper, Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".accesswidener");
    }

    @Override
    public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper tinyRemapper) {
        var outputFile = destinationDirectory.resolve(relativePath.toString());

        var writer = new AccessWidenerWriter();
        var accessWidenerRemapper = new net.fabricmc.accesswidener.AccessWidenerRemapper(
                writer,
                this.remapper,
                fromNamespace,
                toNamespace
        );

        var reader = new AccessWidenerReader(accessWidenerRemapper);
        try {
            reader.read(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
            Files.writeString(outputFile, writer.writeString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}