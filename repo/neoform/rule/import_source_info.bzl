load("@//repo/neoform:java_source_info.bzl", "JavaSourceInfo")
load("@rules_java//java:defs.bzl", "JavaInfo", "java_common")

def _import_source_info_impl(ctx):
    transitive_libraries = []
    for dep in ctx.attr.deps:
        source_info = dep[JavaSourceInfo]
        transitive_libraries.append(source_info.transitive_libraries)

    libraries = depset(transitive = transitive_libraries).to_list()
    java_infos = []
    for library in libraries:
        java_info = JavaInfo(
            output_jar = library,
            compile_jar = library,
        )
        java_infos.append(java_info)

    java_info = java_common.merge(java_infos)

    return [
        DefaultInfo(files = java_info.transitive_compile_time_jars),
        java_info,
    ]

import_source_info = rule(
    implementation = _import_source_info_impl,
    attrs = {
        "deps": attr.label_list(
            providers = [JavaSourceInfo]
        ),
    },
)
