load("@rules_java//java:defs.bzl", "JavaInfo")

SplitResourceInfo = provider(
    doc = "Contains both the classes JAR and resources JAR from splitting",
    fields = {
        "classes_jar": "JAR file containing only .class files",
        "resources_jar": "JAR file containing all non-class resources",
    },
)

def _split_resources_impl(ctx):
    classes_jar = ctx.actions.declare_file(ctx.label.name + "_classes.jar")
    resources_jar = ctx.actions.declare_file(ctx.label.name + "_resources.jar")

    args = ctx.actions.args()
    args.add("--input", ctx.file.input.path)
    args.add("--classes-output", classes_jar.path)
    args.add("--resources-output", resources_jar.path)

    for pattern in ctx.attr.deny_patterns:
        args.add("--deny-pattern", pattern)

    if ctx.attr.generate_manifest:
        args.add("--generate-manifest")
        args.add("--dist-id", ctx.attr.dist_id)
        args.add("--other-dist-id", ctx.attr.other_dist_id)
        args.add("--other-dist-jar", ctx.file.other_dist_jar.path)
        args.add("--mappings", ctx.file.mappings.path)

    inputs = [ctx.file.input]
    if ctx.attr.generate_manifest:
        inputs.append(ctx.file.other_dist_jar)
        inputs.append(ctx.file.mappings)

    ctx.actions.run(
        inputs = inputs,
        outputs = [classes_jar, resources_jar],
        executable = ctx.executable._split_tool,
        arguments = [args],
        progress_message = "Splitting resources from classes for %s" % ctx.label.name,
    )

    return [
        SplitResourceInfo(
            classes_jar = classes_jar,
            resources_jar = resources_jar,
        ),
        JavaInfo(
            output_jar = classes_jar,
            compile_jar = classes_jar,
        ),
        DefaultInfo(
            files = depset([classes_jar]),
        ),
    ]

split_resources = rule(
    implementation = _split_resources_impl,
    attrs = {
        "input": attr.label(
            allow_single_file = [".jar"],
            mandatory = True,
            doc = "Input JAR file to split",
        ),
        "deny_patterns": attr.string_list(
            default = ["META-INF/.*"],
            doc = "List of regex patterns for files to exclude",
        ),
        "generate_manifest": attr.bool(
            default = False,
            doc = "Whether to generate a distribution manifest",
        ),
        "dist_id": attr.string(
            doc = "Distribution ID for manifest generation (e.g., 'client')",
        ),
        "other_dist_id": attr.string(
            doc = "Other distribution ID for manifest generation (e.g., 'server')",
        ),
        "other_dist_jar": attr.label(
            allow_single_file = [".jar"],
            doc = "Other distribution JAR for manifest generation",
        ),
        "mappings": attr.label(
            allow_single_file = True,
            doc = "Mappings file for manifest generation",
        ),
        "_split_tool": attr.label(
            default = Label("//repo/neoform/rule/split_resources"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Splits a JAR file into classes and resources, optionally generating a distribution manifest",
)
