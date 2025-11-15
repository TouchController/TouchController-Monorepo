def do_merge_neoforge_jij(ctx, input_jar, output_jar, deps, executable, label):
    args = ctx.actions.args()
    args.add(input_jar)
    args.add(output_jar)
    dep_files = []
    for dep in deps.keys():
        name = deps[dep]
        jar = dep.files.to_list()[0]
        dep_files.append(jar)
        args.add(name)
        args.add(jar)

    ctx.actions.run(
        inputs = depset(dep_files + [input_jar]),
        outputs = [output_jar],
        executable = executable,
        arguments = [args],
        progress_message = "Create jij JAR %s" % label.name,
        toolchain = "@bazel_tools//tools/jdk:toolchain_type",
    )

def _neoforge_merge_jij_impl(ctx):
    output_jar = ctx.actions.declare_file(ctx.label.name + ".jar")
    input_jar = ctx.files.input[0]

    do_merge_neoforge_jij(
        ctx,
        input_jar,
        output_jar,
        ctx.attr.deps,
        ctx.executable._jij_merger_executable,
        ctx.label,
    )

    return [
        JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
        DefaultInfo(files = depset([output_jar])),
    ]

neoforge_merge_jij = rule(
    implementation = _neoforge_merge_jij_impl,
    attrs = {
        "input": attr.label(
            mandatory = True,
            providers = [DefaultInfo],
            doc = "Input JAR",
        ),
        "deps": attr.label_keyed_string_dict(
            mandatory = True,
            allow_files = [".jar"],
            doc = "JARs to be merged as jar-in-jar"
        ),
        "_jij_merger_executable": attr.label(
            default = Label("@//rule/neoforge/jij_merger"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Merge JAR to a main JAR as Fabric jar-in-jar",
)
