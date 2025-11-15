package top.fifthlight.fabazel.mappingmerger;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.adapter.OuterClassNamePropagator;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MappingMerger {
    private enum MappingFormat {
        TINY_FILE("tiny"),
        TINY_2_FILE("tinyv2"),
        PROGUARD_FILE("proguard");

        private final String name;

        MappingFormat(String name) {
            this.name = name;
        }

        public void read(Reader reader, MappingVisitor visitor) throws IOException {
            switch (this) {
                case TINY_FILE:
                    Tiny1FileReader.read(reader, visitor);
                    break;
                case TINY_2_FILE:
                    Tiny2FileReader.read(reader, visitor);
                    break;
                case PROGUARD_FILE:
                    ProGuardFileReader.read(reader, visitor);
                    break;
            }
        }
    }

    private record NamespacePair(@NotNull String from, @NotNull String to) {

        public static NamespacePair parse(String mapping) {
            var index = mapping.indexOf(":");
                if (index == -1) {
                    throw new IllegalArgumentException("Bad namespace pair: " + mapping);
                }
            var from = mapping.substring(0, index);
            var to = mapping.substring(index + 1);
                if (from.isEmpty()) {
                    throw new IllegalArgumentException("Empty from namespace");
                }
                if (to.isEmpty()) {
                    throw new IllegalArgumentException("Empty to namespace");
                }
                return new NamespacePair(from, to);
            }

        @Override
            public String toString() {
                return from + ":" + to;
            }
        }

    private record InputEntry(@NotNull Path path, @NotNull MappingMerger.MappingFormat format,
                              @NotNull Map<String, String> namespaceMapping, @Nullable String sourceNamespace) {

        @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
            var that = (InputEntry) o;
                return Objects.equals(path, that.path) && format == that.format && Objects.equals(namespaceMapping, that.namespaceMapping) && Objects.equals(sourceNamespace, that.sourceNamespace);
            }

        @Override
            public String toString() {
                return "InputEntry{" +
                        "path=" + path +
                        ", format=" + format +
                        ", namespaceMapping=" + namespaceMapping +
                        ", sourceNamespace='" + sourceNamespace + '\'' +
                        '}';
            }
        }

    public static void main(String[] args) throws IOException {
        MappingFormat format = null;
        var namespaceMappings = new HashMap<String, String>();
        var completeNamespace = new HashMap<String, String>();
        List<InputEntry> inputEntries = new ArrayList<>();
        Path outputPath = null;
        String sourceNamespace = null;
        for (var argIndex = 0; argIndex < args.length; argIndex++) {
            var arg = args[argIndex];
            if (arg.startsWith("--")) {
                var name = arg.substring(2);
                if (argIndex >= args.length - 1) {
                    throw new IllegalArgumentException("No value for argument: " + arg);
                }
                var value = args[argIndex + 1];
                argIndex++;
                switch (name) {
                    case "format":
                        if (format != null) {
                            throw new IllegalArgumentException("Mapping format is already specified: " + format);
                        }
                        format = Arrays.stream(MappingFormat.values())
                                .filter(type -> type.name.equals(value))
                                .findAny()
                                .orElseThrow(() -> {
                                    var availableMappingTypes = Arrays.stream(MappingFormat.values())
                                            .map(type -> type.name)
                                            .collect(Collectors.joining("\n"));
                                    return new IllegalArgumentException(
                                            "Bad mapping type: " + value + "\n" + "Available mappings type:" + "\n" + availableMappingTypes
                                    );
                                });
                        break;
                    case "source-namespace":
                        if (sourceNamespace != null) {
                            throw new IllegalArgumentException("Source Namespace is already specified: " + sourceNamespace);
                        }
                        sourceNamespace = value;
                        break;
                    case "complete_namespace":
                        var completePair = NamespacePair.parse(value);
                        if (completeNamespace.containsKey(completePair.from())) {
                            throw new IllegalArgumentException("Complete namespace is already specified: " + completePair);
                        }
                        completeNamespace.put(completePair.from(), completePair.to());
                        break;
                    case "namespace-mapping":
                        var namespacePair = NamespacePair.parse(value);
                        if (namespaceMappings.containsKey(namespacePair.from())) {
                            throw new IllegalArgumentException("Namespace mapping is already specified: " + namespacePair);
                        }
                        namespaceMappings.put(namespacePair.from(), namespacePair.to());
                        break;
                    default:
                        throw new IllegalArgumentException("Bad argument: " + name);
                }
            } else if (argIndex == args.length - 1) {
                outputPath = Path.of(arg);
            } else {
                if (format == null) {
                    throw new IllegalArgumentException("No format specified for mapping: " + arg);
                }
                var path = Path.of(arg);
                inputEntries.add(new InputEntry(path,
                        format,
                        Collections.unmodifiableMap(namespaceMappings),
                        sourceNamespace));
                format = null;
                namespaceMappings = new HashMap<>();
                sourceNamespace = null;
            }
        }

        if (inputEntries.isEmpty() || outputPath == null) {
            System.out.println("Usage: MappingMerger [--source-namespace <namespace>]... <--format=<mapping type> [--namespace-mapping <from:to>]... [--completeNamespace <from:to>]... <mapping path>>... <output file>");
            return;
        }

        var destinationTree = new MemoryMappingTree();
        for (var entry : inputEntries) {
            var visitor = getMappingVisitor(entry, destinationTree);
            try (var reader = Files.newBufferedReader(entry.path())) {
                entry.format().read(reader, visitor);
            }
        }
        try (var writer = Files.newBufferedWriter(outputPath)) {
            MappingVisitor visitor = new Tiny2FileWriter(writer, false);
            if (!completeNamespace.isEmpty()) {
                visitor = new MappingNsCompleter(visitor, completeNamespace);
            }
            visitor = new OuterClassNamePropagator(visitor);
            destinationTree.accept(visitor);
        }
    }

    private static MappingVisitor getMappingVisitor(InputEntry entry, MemoryMappingTree destinationTree) {
        MappingVisitor visitor = destinationTree;
        if (entry.sourceNamespace() != null) {
            visitor = new MappingSourceNsSwitch(visitor, entry.sourceNamespace());
        }
        if (!entry.namespaceMapping().isEmpty()) {
            visitor = new MappingNsRenamer(visitor, entry.namespaceMapping());
        }
        return visitor;
    }
}
