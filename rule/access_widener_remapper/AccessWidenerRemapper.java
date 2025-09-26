package top.fifthlight.armorstand;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.mappingio.extras.MappingTreeRemapper;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AccessWidenerRemapper {
    public static void main(String[] args) throws IOException {
        var inputFile = Path.of(args[0]);
        var outputFile = Path.of(args[1]);
        var mappingFile = Path.of(args[2]);
        var fromNamespace = args[3];
        var toNamespace = args[4];

        var mappingTree = new MemoryMappingTree();
        try (var reader = Files.newBufferedReader(mappingFile)) {
            Tiny2FileReader.read(reader, mappingTree);
        }
        var remapper = new MappingTreeRemapper(mappingTree, fromNamespace, toNamespace);

        try (var writer = Files.newOutputStream(outputFile)) {
            var accessWidenerWriter = new AccessWidenerWriter();
            var accessWidenerRemapper = new net.fabricmc.accesswidener.AccessWidenerRemapper(accessWidenerWriter, remapper, fromNamespace, toNamespace);
            var accessWidenerReader = new AccessWidenerReader(accessWidenerRemapper);
            try (var reader = Files.newBufferedReader(inputFile)) {
                accessWidenerReader.read(reader);
            }
            writer.write(accessWidenerWriter.write());
        }
    }
}