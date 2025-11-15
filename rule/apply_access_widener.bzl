load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _apply_access_widener_impl(ctx):
    output_file = ctx.actions.declare_file("_access_widened/%s.jar" % ctx.label.name)
    args = ctx.actions.args()

    args.add_all([
        ctx.file.input.path,
        output_file.path,
    ])
    args.add_all(ctx.files.srcs)

    ctx.actions.run(
        inputs = [ctx.file.input] + ctx.files.srcs,
        outputs = [output_file],
        executable = ctx.executable._transformer_bin,
        arguments = [args],
        progress_message = "Applying access wideners for %s" % ctx.label.name,
    )

    return [
        JavaInfo(
            output_jar = output_file,
            compile_jar = output_file,
            neverlink = ctx.attr.neverlink,
        ),
        DefaultInfo(files = depset([output_file])),
    ]

apply_access_widener = rule(
    implementation = _apply_access_widener_impl,
    attrs = {
        "input": attr.label(
            allow_single_file = [".jar", ".zip"],
            mandatory = True,
            doc = "Input JAR file",
        ),
        "srcs": attr.label_list(
            allow_files = [".accesswidener"],
            mandatory = True,
            doc = "List of access widener files",
        ),
        "neverlink": attr.bool(
            default = False,
            mandatory = False,
        ),
        "_transformer_bin": attr.label(
            default = Label("//rule/access_widener_transformer"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Apply access transformer on JAR.",
)
