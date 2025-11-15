load("@rules_java//java:defs.bzl", "java_common")
load("@rules_java//java:defs.bzl", "JavaInfo")

def _decompile_jar_impl(ctx):
    output_file = ctx.actions.declare_file("%s.jar" % ctx.label.name)
    merged_input = java_common.merge([dep[JavaInfo] for dep in ctx.attr.inputs])

    args = ctx.actions.args()
    args.add_all(merged_input.full_compile_jars.to_list())
    args.add(output_file)

    ctx.actions.run(
        inputs = merged_input.full_compile_jars,
        outputs = [output_file],
        executable = ctx.executable._vineflower_bin,
        arguments = [args],
        progress_message = "Decompiling %s" % ctx.label.name,
    )

    return [DefaultInfo(files = depset([output_file]))]

decompile_jar = rule(
    implementation = _decompile_jar_impl,
    attrs = {
        "inputs": attr.label_list(
            mandatory = True,
            doc = "Input JAR files",
        ),
        "_vineflower_bin": attr.label(
            default = Label("//rule/vineflower"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Decompile JAR archives with vineflower.",
)
