package top.fifthlight.mergetools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import top.fifthlight.mergetools.processor.ActualData;
import top.fifthlight.mergetools.processor.ExpectData;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class ExpectActualMerger {
    private static final HashMap<String, ExpectData> expectDataMap = new HashMap<>();
    private static final HashMap<String, ActualData> actualDataMap = new HashMap<>();
    private static final ArrayList<JarFile> jarFiles = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String expectPrefix = "META-INF/expects/";
    private static final String actualPrefix = "META-INF/actuals/";
    private static Path outputPath;

    private static String internalNameToPath(String internalName) {
        if (!internalName.startsWith("L") || !internalName.endsWith(";")) {
            throw new IllegalArgumentException("Invalid binary name: " + internalName);
        }
        return internalName.substring(1, internalName.length() - 1);
    }

    private static HashMap<String, MergeEntry> preprocess(String[] args) throws IOException {
        var mergeEntries = new HashMap<String, MergeEntry>();
        var factoryClasses = new HashSet<String>();

        outputPath = Path.of(args[0]);

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
                    mergeEntries.put(entryPath, new MergeEntry.ResourceFile(Path.of(filePath)));
                }

                default -> {
                    var inputPath = Path.of(args[i]);
                    var jarFile = new JarFile(inputPath.toFile());
                    jarFiles.add(jarFile);

                    var enumerator = jarFile.entries();
                    JarEntry entry;
                    while (enumerator.hasMoreElements()) {
                        entry = enumerator.nextElement();
                        var name = entry.getName();
                        if (name.startsWith(expectPrefix) && name.endsWith(".json")) {
                            try (var inputStream = new BufferedInputStream(jarFile.getInputStream(entry));
                                 var reader = new InputStreamReader(inputStream)) {
                                var expectData = mapper.readValue(reader, ExpectData.class);
                                var interfaceFullQualifiedName = name.substring(expectPrefix.length(), name.length() - ".json".length());
                                var interfaceClassPath = internalNameToPath(expectData.interfaceName());
                                var interfaceFactoryPath = interfaceClassPath + "Factory.class";
                                mergeEntries.put(interfaceFactoryPath, new MergeEntry.ExpectManifest(interfaceFullQualifiedName, expectData));
                                expectDataMap.put(interfaceFullQualifiedName, expectData);
                                factoryClasses.add(interfaceFactoryPath);
                            }
                        } else if (name.startsWith(actualPrefix) && name.endsWith(".json")) {
                            try (var inputStream = new BufferedInputStream(jarFile.getInputStream(entry));
                                 var reader = new InputStreamReader(inputStream)) {
                                var actualData = mapper.readValue(reader, ActualData.class);
                                var interfaceFullQualifiedName = name.substring(actualPrefix.length(), name.length() - ".json".length());
                                if (actualDataMap.containsKey(interfaceFullQualifiedName)) {
                                    throw new IllegalStateException("Duplicate actual data: " + interfaceFullQualifiedName);
                                }
                                actualDataMap.put(interfaceFullQualifiedName, actualData);
                            }
                        } else if (!entry.isDirectory()) {
                            var hasFile = mergeEntries.containsKey(name);
                            var isFactoryFile = factoryClasses.contains(name);
                            var isClassFile = name.endsWith(".class");
                            if (hasFile && !isFactoryFile && isClassFile) {
                                throw new IllegalStateException("Duplicate entry: " + name);
                            }
                            if (!isFactoryFile) {
                                mergeEntries.put(name, new MergeEntry.JarItem(jarFile, entry));
                            }
                        }
                    }
                }
            }
        }
        return mergeEntries;
    }

    private static List<Map.Entry<String, MergeEntry>> sort(HashMap<String, MergeEntry> mergeEntries) {
        for (var expectEntry : expectDataMap.entrySet()) {
            var key = expectEntry.getKey();
            var actualData = actualDataMap.get(key);
            if (actualData == null) {
                throw new IllegalStateException("Missing actual class for: " + key);
            }

            var actualSpiFactoryPath = internalNameToPath(actualData.spiFactoryName()) + ".class";
            if (!mergeEntries.containsKey(actualSpiFactoryPath)) {
                throw new IllegalStateException("Missing actual spi factory: " + actualSpiFactoryPath);
            }
            mergeEntries.remove(actualSpiFactoryPath);

            var spiManifestPath = "META-INF/services/" + key + "$Factory";
            if (!mergeEntries.containsKey(spiManifestPath)) {
                throw new IllegalStateException("Missing spi manifest: " + spiManifestPath);
            }
            mergeEntries.remove(spiManifestPath);
        }

        return mergeEntries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
    }

    private static void setJarEntryTime(JarEntry entry) {
        entry.setCreationTime(FileTime.fromMillis(0));
        entry.setLastAccessTime(FileTime.fromMillis(0));
        entry.setLastModifiedTime(FileTime.fromMillis(0));
        entry.setTimeLocal(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC));
    }

    private record MethodPair(String parameterTypes, String name) {
        public MethodPair(ExpectData.Constructor constructor) {
            this(Arrays.stream(constructor.parameters()).map(ExpectData.Constructor.Parameter::type).collect(Collectors.joining()), constructor.name());
        }

        public MethodPair(ActualData.Constructor constructor) {
            this(Arrays.stream(constructor.parameters()).map(ActualData.Constructor.Parameter::type).collect(Collectors.joining()), constructor.name());
        }
    }

    @SuppressWarnings("resource")
    private static void writeJar(List<Map.Entry<String, MergeEntry>> entries) throws IOException {
        try (var outputStream = new JarOutputStream(Files.newOutputStream(outputPath))) {
            for (var entry : entries) {
                var value = entry.getValue();
                switch (value) {
                    case MergeEntry.ExpectManifest expectManifest -> {
                        var outputEntry = new JarEntry(entry.getKey());
                        setJarEntryTime(outputEntry);
                        outputStream.putNextEntry(outputEntry);

                        var expectData = expectManifest.data();
                        var actualData = actualDataMap.get(expectManifest.interfaceFullQualifiedName());
                        var expectBinaryName = expectData.interfaceName();
                        var interfaceTypeName = internalNameToPath(expectBinaryName);

                        var expectConstructors = Arrays.stream(expectData.constructors()).collect(Collectors.toMap(
                                MethodPair::new,
                                constructor -> constructor,
                                (a, b) -> {
                                    throw new IllegalStateException("Duplicate expect constructors: " + a + ", " + b);
                                }
                        ));
                        var actualConstructors = Arrays.stream(actualData.constructors()).collect(Collectors.toMap(
                                MethodPair::new,
                                constructor -> constructor,
                                (a, b) -> {
                                    throw new IllegalStateException("Duplicate actual constructors: " + a + ", " + b);
                                }
                        ));

                        // Time of ASM magic
                        var classWriter = new ClassWriter(0);
                        classWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, interfaceTypeName + "Factory", null, "java/lang/Object", null);
                        for (var expectConstructorPair : expectConstructors.entrySet()) {
                            var methodPair = expectConstructorPair.getKey();
                            var expectConstructor = expectConstructorPair.getValue();
                            var actualConstructor = actualConstructors.get(methodPair);
                            if (actualConstructor == null) {
                                throw new IllegalStateException("No actual constructor found for method pair " + methodPair);
                            }

                            var generatedMethodDescriptor = "(" + methodPair.parameterTypes() + ")" + expectBinaryName;
                            var methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, expectConstructor.name(), generatedMethodDescriptor, null, null);
                            methodVisitor.visitCode();

                            var actualClassName = internalNameToPath(actualData.implementationName());
                            switch (actualConstructor.type()) {
                                case CONSTRUCTOR -> {
                                    methodVisitor.visitTypeInsn(Opcodes.NEW, actualClassName);
                                    methodVisitor.visitInsn(Opcodes.DUP);
                                }
                                case STATIC_METHOD -> {
                                }
                            }

                            var parameters = expectConstructor.parameters();
                            var variableLabels = new Label[parameters.length];
                            for (var i = 0; i < parameters.length; i++) {
                                var parameter = parameters[i];

                                var label = new Label();
                                variableLabels[i] = label;
                                methodVisitor.visitLabel(label);

                                var objType = parameter.type().charAt(0);
                                switch (objType) {
                                    case 'L' -> methodVisitor.visitVarInsn(Opcodes.ALOAD, i);
                                    case 'Z', 'B', 'S', 'C', 'I' -> methodVisitor.visitVarInsn(Opcodes.ILOAD, i);
                                    case 'J' -> methodVisitor.visitVarInsn(Opcodes.LLOAD, i);
                                    case 'F' -> methodVisitor.visitVarInsn(Opcodes.FLOAD, i);
                                    case 'D' -> methodVisitor.visitVarInsn(Opcodes.DLOAD, i);
                                }
                            }

                            switch (actualConstructor.type()) {
                                case CONSTRUCTOR -> {
                                    var constructorDescriptor = "(" + methodPair.parameterTypes() + ")V";
                                    methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, actualClassName, "<init>", constructorDescriptor, false);
                                }
                                case STATIC_METHOD -> {
                                    var actualMethodDescriptor = "(" + methodPair.parameterTypes() + ")" + actualConstructor.returnType();
                                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, actualClassName, expectConstructor.name(), actualMethodDescriptor, false);
                                }
                            }
                            var endLabel = new Label();
                            methodVisitor.visitLabel(endLabel);
                            methodVisitor.visitInsn(Opcodes.ARETURN);

                            for (var i = 0; i < parameters.length; i++) {
                                var parameter = parameters[i];
                                methodVisitor.visitLocalVariable(parameter.name(), parameter.type(), null, variableLabels[i], endLabel, i);
                            }
                            methodVisitor.visitMaxs(parameters.length + 2, parameters.length);

                            methodVisitor.visitEnd();
                        }
                        classWriter.visitEnd();

                        var classData = classWriter.toByteArray();
                        outputStream.write(classData);
                        outputStream.closeEntry();
                    }
                    case MergeEntry.JarItem jarItem -> {
                        var outputEntry = new JarEntry(entry.getKey());
                        setJarEntryTime(outputEntry);
                        outputStream.putNextEntry(outputEntry);
                        try (var inputStream = jarItem.jarFile().getInputStream(jarItem.entry())) {
                            inputStream.transferTo(outputStream);
                        }
                        outputStream.closeEntry();
                    }
                    case MergeEntry.ResourceFile resourceFile -> {
                        var outputEntry = new JarEntry(entry.getKey());
                        setJarEntryTime(outputEntry);
                        outputStream.putNextEntry(outputEntry);
                        try (var inputStream = Files.newInputStream(resourceFile.path())) {
                            inputStream.transferTo(outputStream);
                        }
                        outputStream.closeEntry();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            var mergeEntries = preprocess(args);
            var outputEntries = sort(mergeEntries);
            writeJar(outputEntries);
        } finally {
            for (var file : jarFiles) {
                file.close();
            }
        }
    }
}