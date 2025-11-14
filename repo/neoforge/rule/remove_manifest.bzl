def _remove_manifest_impl(ctx):
    output_file = ctx.actions.declare_file(ctx.label.name + ".jar")

    args = ctx.actions.args()
    args.add(ctx.file.src.path)
    args.add(output_file.path)

    ctx.actions.run(
        executable = ctx.executable._manifest_remover_tool,
        arguments = [args],
        inputs = [ctx.file.src],
        outputs = [output_file],
    )

    return [DefaultInfo(files = depset([output_file]))]

remove_manifest = rule(
    implementation = _remove_manifest_impl,
    attrs = {
        "src": attr.label(
            mandatory = True,
            allow_single_file = [".jar", ".zip"],
        ),
        "_manifest_remover_tool": attr.label(
            default = "//repo/neoforge/rule/manifest_remover",
            executable = True,
            cfg = "exec",
        ),
    }
)