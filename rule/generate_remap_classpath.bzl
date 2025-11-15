def _generate_remap_classpath_impl(ctx):
    output_file = ctx.actions.declare_file(ctx.label.name + ".txt")
    merged_deps = java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])

    ctx.actions.write(
        output = output_file,
        content = ":".join([ctx.attr.prefix + file.path for file in merged_deps.transitive_runtime_jars.to_list()])
    )

    return [DefaultInfo(files = depset([output_file]))]

generate_remap_classpath = rule(
    implementation = _generate_remap_classpath_impl,
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            providers = [JavaInfo],
            doc = "Input files",
        ),
        "prefix": attr.string(
            default = "",
            doc = "Prefix of paths",
        )
    },
    doc = "Generate remap classpath file for Fabric development environment.",
)
