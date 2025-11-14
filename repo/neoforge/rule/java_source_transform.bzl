load("@//repo/neoform:java_source_info.bzl", "JavaSourceInfo")

JavaSourceTransformInfo = provider(
    doc = "Provides information about a Java source transformation",
    fields = {
        "output_jar": "The output JAR file",
        "problems_report": "The problems report JSON file",
    },
)

def _java_source_transform_impl(ctx):
    output_jar = ctx.actions.declare_file(ctx.label.name + ".srcjar")
    problems_report = ctx.actions.declare_file(ctx.label.name + ".problems.json")

    libraries_file = ctx.actions.declare_file(ctx.label.name + ".libraries.txt")
    libraries = []
    source_info = ctx.attr.input[JavaSourceInfo]
    input_jar = source_info.source_jar
    for library in source_info.transitive_libraries.to_list():
        libraries.append(library)
    ctx.actions.write(
        output = libraries_file,
        content = "\n".join([library.path for library in libraries]),
    )

    args = ctx.actions.args()
    args.add("--problems-report", problems_report.path)
    args.add("--libraries-list", libraries_file.path)
    args.add("--in-format", "ARCHIVE")
    args.add("--out-format", "ARCHIVE")
    if len(ctx.files.access_transformers) > 0:
        args.add("--enable-accesstransformers")
        for access_transformer in ctx.files.access_transformers:
            args.add("--access-transformer", access_transformer.path)
    args.add(input_jar.path)
    args.add(output_jar.path)

    ctx.actions.run(
        executable = ctx.executable._java_source_transformer_tool,
        arguments = [args],
        inputs = [input_jar, libraries_file] + libraries + ctx.files.access_transformers,
        outputs = [output_jar, problems_report],
    )

    return [
        DefaultInfo(
            files = depset([output_jar])
        ),
        JavaSourceTransformInfo(
            output_jar = output_jar,
            problems_report = problems_report,
        ),
        JavaSourceInfo(
            source_jar = output_jar,
            deps = [source_info],
        )
    ]

java_source_transform = rule(
    implementation = _java_source_transform_impl,
    attrs = {
        "input": attr.label(
            doc = "The source JAR file to transform",
            providers = [JavaSourceInfo],
            mandatory = True,
        ),
        "access_transformers": attr.label_list(
            doc = "The access transformer files",
            allow_files = True,
            default = [],
        ),
        "_java_source_transformer_tool": attr.label(
            default = Label("//repo/neoforge/rule/java_source_transformer"),
            executable = True,
            cfg = "exec",
        ),
    },
)