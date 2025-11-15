load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _remap_access_widener_impl(ctx):
    output_file = ctx.actions.declare_file("%s.accesswidener" % ctx.label.name)
    args = ctx.actions.args()

    args.add(ctx.file.src.path)
    args.add(output_file.path)
    args.add(ctx.file.mapping.path)
    args.add(ctx.attr.from_namespace)
    args.add(ctx.attr.to_namespace)

    ctx.actions.run(
        inputs = [ctx.file.src, ctx.file.mapping],
        outputs = [output_file],
        executable = ctx.executable._remapper_bin,
        arguments = [args],
        progress_message = "Remapping access widener %s" % ctx.label.name,
    )

    return [DefaultInfo(files = depset([output_file]))]

remap_access_widener = rule(
    implementation = _remap_access_widener_impl,
    attrs = {
        "src": attr.label(
            allow_single_file = [".accesswidener"],
            mandatory = True,
            doc = "Access widener file",
        ),
        "mapping": attr.label(
            allow_single_file = [".tiny"],
            mandatory = True,
            doc = "Mapping file",
        ),
        "from_namespace": attr.string(
            mandatory = True,
        ),
        "to_namespace": attr.string(
            mandatory = True,
        ),
        "_remapper_bin": attr.label(
            default = Label("//rule/access_widener_remapper"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Remap access transformer.",
)
