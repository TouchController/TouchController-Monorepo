load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _convert_access_widener_impl(ctx):
    output_file = ctx.actions.declare_file(ctx.attr.output_name)
    args = ctx.actions.args()

    args.add(ctx.file.src.path)
    args.add(output_file.path)

    ctx.actions.run(
        inputs = [ctx.file.src],
        outputs = [output_file],
        executable = ctx.executable._converter_bin,
        arguments = [args],
        progress_message = "Converting access widener to access widener for %s" % ctx.label.name,
    )

    return [DefaultInfo(files = depset([output_file]))]

convert_access_widener = rule(
    implementation = _convert_access_widener_impl,
    attrs = {
        "src": attr.label(
            allow_single_file = [".accesswidener"],
            mandatory = True,
            doc = "Access widener file",
        ),
        "output_name": attr.string(
            mandatory = True,
        ),
        "_converter_bin": attr.label(
            default = Label("//rule/neoforge/access_widener_converter"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Convert Fabric access transformer to Forge access widener.",
)
