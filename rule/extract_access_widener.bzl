def _extract_access_widener_impl(ctx):
    output_file = ctx.actions.declare_file("_extracted/%s.accesswidener" % ctx.label.name)
    args = ctx.actions.args()

    args.add(output_file.path)

    input_jar_depsets = []
    for input_target in ctx.attr.inputs:
        input_java_info = input_target[JavaInfo]
        if ctx.attr.extract_transitive_deps:
            input_jar_depsets.append(input_java_info.compile_jars)
            input_jar_depsets.append(input_java_info.transitive_compile_time_jars)
        else:
            input_jar_depsets.append(depset(input_java_info.runtime_output_jars))

    input_jars = depset(transitive = input_jar_depsets)
    for input_jar in input_jars.to_list():
        args.add(input_jar.path)

    args.use_param_file("@%s")

    ctx.actions.run(
        inputs = input_jars,
        outputs = [output_file],
        executable = ctx.executable._extractor_bin,
        arguments = [args],
        progress_message = "Extracting access widener for %s" % ctx.label.name,
    )

    return [DefaultInfo(files = depset([output_file]))]

extract_access_widener = rule(
    implementation = _extract_access_widener_impl,
    attrs = {
        "inputs": attr.label_list(
            providers = [JavaInfo],
            mandatory = True,
            doc = "Input JAR files.",
        ),
        "extract_transitive_deps": attr.bool(
            default = True,
            doc = "Extract transitive dependencies",
        ),
        "_extractor_bin": attr.label(
            default = Label("//rule/access_widener_extractor"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Extract access widener from Fabric mods.",
)
