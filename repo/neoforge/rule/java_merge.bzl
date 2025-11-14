load("@rules_java//java:defs.bzl", "java_common", "JavaInfo")

def _java_merge_impl(ctx):
    deps_java_infos = [dep[JavaInfo] for dep in ctx.attr.deps if JavaInfo in dep]

    merged_java_info = java_common.merge(deps_java_infos)
    merged_java_info = java_common.make_non_strict(merged_java_info)

    return [
        merged_java_info,
        DefaultInfo(files = merged_java_info.full_compile_jars),
    ]

java_merge = rule(
    implementation = _java_merge_impl,
    attrs = {
        "deps": attr.label_list(
            providers = [JavaInfo],
            mandatory = True,
        ),
    },
    provides = [JavaInfo],
)