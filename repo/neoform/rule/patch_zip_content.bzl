load("@rules_java//java:defs.bzl", "JavaInfo")
load("@//repo/neoform:java_source_info.bzl", "JavaSourceInfo")

PatchZipInfo = provider(
    doc = "Contains both the patched and rejected JARs",
    fields = {
        "patched_jar": "JAR file containing patched sources",
        "rejects_jar": "JAR file containing rejected chunks",
    },
)

def _patch_zip_content_impl(ctx):
    patched_jar = ctx.actions.declare_file(ctx.label.name + "_patched.jar")
    rejects_jar = ctx.actions.declare_file(ctx.label.name + "_rejects.jar")

    input_deps = []
    if JavaSourceInfo in ctx.attr.input:
        input_deps.append(ctx.attr.input[JavaSourceInfo])

    args = ctx.actions.args()
    args.add(ctx.file.input.path, ctx.file.patches.path)
    args.add("--prefix", ctx.attr.prefix)
    args.add("--patch")
    args.add("--archive", "ZIP")
    args.add("--output", patched_jar.path)
    args.add("--log-level", "WARN")
    args.add("--mode", "OFFSET")
    args.add("--archive-rejects", "ZIP")
    args.add("--reject", rejects_jar.path)
    args.add("--base-path-prefix", ctx.attr.base_path_prefix)
    args.add("--modified-path-prefix", ctx.attr.modified_path_prefix)

    ctx.actions.run(
        inputs = [ctx.file.input, ctx.file.patches],
        outputs = [patched_jar, rejects_jar],
        executable = ctx.executable._patch_zip_content_tool,
        arguments = [args],
    )

    return [
        PatchZipInfo(
            patched_jar = patched_jar,
            rejects_jar = rejects_jar,
        ),
        DefaultInfo(
            files = depset([patched_jar]),
        ),
        JavaSourceInfo(
            source_jar = patched_jar,
            deps = input_deps,
        ),
    ]

patch_zip_content = rule(
    implementation = _patch_zip_content_impl,
    attrs = {
        "input": attr.label(
            allow_single_file = [".jar"],
            providers = [[], [JavaSourceInfo]],
            mandatory = True,
            doc = "Input JAR file to patch",
        ),
        "prefix": attr.string(
            mandatory = True,
            doc = "Prefix to apply to all files",
        ),
        "patches": attr.label(
            allow_single_file = True,
            mandatory = True,
            doc = "Patch file",
        ),
        "base_path_prefix": attr.string(
            default = "a/",
            doc = "Base path prefix to apply to all files",
        ),
        "modified_path_prefix": attr.string(
            default = "b/",
            doc = "Modified path prefix to apply to all files",
        ),
        "_patch_zip_content_tool": attr.label(
            default = Label("//repo/neoform/rule/patch_zip_content"),
            executable = True,
            cfg = "exec",
        ),
    },
)
