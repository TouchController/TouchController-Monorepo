load("@rules_java//java:defs.bzl", "java_common", "JavaInfo")

def _merge_jar_impl(ctx):
    output_jar = ctx.actions.declare_file(ctx.label.name + ".jar")

    merged_deps = java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])

    args = ctx.actions.args()
    args.add(output_jar)
    resource_files = []
    for resource in ctx.attr.resources.keys():
        strip = ctx.attr.resources[resource]
        files = resource.files.to_list()
        resource_files = resource_files + files
        args.add("--strip")
        args.add(strip)
        if len(files) == 0:
            fail("Resource label without resource: " + str(resource.label))
        for file in files:
            args.add("--resource")
            args.add(file)
    args.add_all(merged_deps.full_compile_jars.to_list())

    ctx.actions.run(
        inputs = depset(
            direct = resource_files,
            transitive = [merged_deps.full_compile_jars],
        ),
        outputs = [output_jar],
        executable = ctx.executable._merge_jar_executable,
        arguments = [args],
        progress_message = "Merging JAR %s" % ctx.label.name,
        toolchain = "@bazel_tools//tools/jdk:toolchain_type",
    )

    return [
        JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
        DefaultInfo(files = depset([output_jar])),
    ]

merge_jar = rule(
    implementation = _merge_jar_impl,
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            providers = [JavaInfo],
            doc = "Input JARs to be merged",
        ),
        "resources": attr.label_keyed_string_dict(
            mandatory = False,
            allow_files = True,
            default = {},
            doc = "Resource to be merged, with perfix to strip",
        ),
        "_merge_jar_executable": attr.label(
            default = Label("@//rule/merge_jar"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Merge JARs"
)
