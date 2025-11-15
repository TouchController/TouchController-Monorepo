package top.fifthlight.fabazel.remapper;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
import top.fifthlight.bazel.worker.api.WorkRequest;
import top.fifthlight.bazel.worker.api.Worker;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

public class TinyRemapperWorker extends Worker implements AutoCloseable {
    public static void main(String[] args) throws Exception {
        try (var worker = new TinyRemapperWorker()) {
            worker.run();
        }
    }

    private final MappingManager mappings = new MappingManager();
    private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

    @Override
    protected int handleRequest(WorkRequest request, PrintWriter out) {
        Map<String, WorkRequest.Input> inputs = new HashMap<>();
        for (var input : request.inputs()) {
            inputs.put(input.path(), input);
        }

        Function<String, String> getInputFileHash = (file) -> {
            var inputInfo = inputs.get(file);
            if (inputInfo != null) {
                return inputInfo.digest();
            } else {
                throw new RuntimeException("Bad input file: " + file);
            }
        };

        try {
            List<String> parameters = new ArrayList<>();
            List<String> arguments = new ArrayList<>();

            for (var arg : request.arguments()) {
                if (arg.startsWith("--")) {
                    parameters.add(arg);
                } else {
                    arguments.add(arg);
                }
            }

            var mixin = false;
            var fixPackageAccess = false;
            var remapAccessWidener = false;
            var removeJarInJar = false;
            String accessWidenerSourceNamespace = null;

            for (var parameter : parameters) {
                var name = parameter.substring(2); // removePrefix("--")
                var accessWidenerSourceArg = "access_widener_from_namespace_";
                if (name.startsWith(accessWidenerSourceArg)) {
                    accessWidenerSourceNamespace = name.substring(accessWidenerSourceArg.length());
                    continue;
                }
                switch (name) {
                    case "mixin":
                        mixin = true;
                        break;
                    case "fix_package_access":
                        fixPackageAccess = true;
                        break;
                    case "remap_access_widener":
                        remapAccessWidener = true;
                        break;
                    case "remove_jar_in_jar":
                        removeJarInJar = true;
                        break;
                }
            }

            if (arguments.size() < 5) {
                out.println("Bad count of arguments: " + arguments.size() + ", at least 5");
                return 1;
            }

            var inputJar = arguments.get(0);
            var outputJar = arguments.get(1);
            var mappingPath = arguments.get(2);
            var fromNamespace = arguments.get(3);
            var toNamespace = arguments.get(4);

            var classpath = arguments.subList(5, arguments.size())
                    .stream()
                    .map(Paths::get)
                    .toList();

            if (accessWidenerSourceNamespace == null || accessWidenerSourceNamespace.isEmpty()) {
                accessWidenerSourceNamespace = fromNamespace;
            }

            var mappingArgument = new MappingManager.Argument(
                    Paths.get(mappingPath),
                    getInputFileHash.apply(mappingPath),
                    fromNamespace,
                    toNamespace
            );

            var entry = mappings.get(mappingArgument);

            var logger = new PrintLogger(out);

            var builder = TinyRemapper.newRemapper(logger)
                    .withMappings(entry.getProvider())
                    .renameInvalidLocals(true)
                    .rebuildSourceFilenames(true)
                    .invalidLvNamePattern(MC_LV_PATTERN)
                    .resolveMissing(true)
                    .inferNameFromSameLvIndex(true);
            if (mixin) {
                builder.extension(new MixinExtension());
            }
            if (fixPackageAccess) {
                builder.fixPackageAccess(true);
                builder.checkPackageAccess(true);
            }

            var remapper = builder.build();

            var input = Paths.get(inputJar);
            var outputTempFs = Jimfs.newFileSystem(Configuration.unix());
            var outputTempRoot = outputTempFs.getPath("/");
            try {
                var outputBuilder = new OutputConsumerPath.Builder(outputTempRoot);
                outputBuilder.assumeArchive(false);
                var output = outputBuilder.build();

                var nonClassFilesProcessors = new ArrayList<OutputConsumerPath.ResourceRemapper>();
                if (removeJarInJar) {
                    nonClassFilesProcessors.add(JarInJarRemover.INSTANCE);
                }
                nonClassFilesProcessors.addAll(NonClassCopyMode.FIX_META_INF.remappers);
                if (remapAccessWidener) {
                    nonClassFilesProcessors.add(
                            new AccessWidenerRemapper(
                                    entry.getRemapper(),
                                    accessWidenerSourceNamespace,
                                    toNamespace
                            )
                    );
                }

                output.addNonClassFiles(input, remapper, nonClassFilesProcessors);
                remapper.readInputs(input);
                for (var cp : classpath) {
                    remapper.readClassPath(cp);
                }
                remapper.apply(output);
                output.close();
            } finally {
                remapper.finish();
            }

            try (var outputJarStream = new JarOutputStream(Files.newOutputStream(Paths.get(outputJar)));
                 var outputFilePaths = Files.walk(outputTempRoot)) {
                outputFilePaths
                        .sorted()
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            var jarEntry = new JarEntry(outputTempRoot.relativize(path).toString());
                            jarEntry.setTime(0L);
                            try {
                                outputJarStream.putNextEntry(jarEntry);
                                Files.copy(path, outputJarStream);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
            } finally {
                outputTempFs.close();
            }

            return 0;
        } catch (Exception ex) {
            ex.printStackTrace(out);
            return 1;
        }
    }

    @Override
    public void close() {
        mappings.close();
    }
}