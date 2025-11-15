load("@rules_java//java:defs.bzl", "java_library")
load("@bazel_skylib//rules:expand_template.bzl", "expand_template")

def _fabric_mod_json_jar_impl(name, visibility, src, resource_strip_prefix, substitutions):
    expand_template(
        name = name + "_expanded",
        template = src,
        substitutions = substitutions,
        out = "fabric.mod.json",
    )
    java_library(
        name = name,
        visibility = visibility,
        resources = [name + "_expanded"],
        resource_strip_prefix = resource_strip_prefix,
    )

fabric_mod_json_jar = macro(
    attrs = {
        "src": attr.label(
            mandatory = True,
            allow_single_file = [".json"],
            doc = "Input fabric.mod.json file",
        ),
        "resource_strip_prefix": attr.string(
            mandatory = True,
        ),
        "substitutions": attr.string_dict(
            mandatory = True,
            doc = "A dictionary mapping strings to their substitutions.",
        ),
    },
    implementation = _fabric_mod_json_jar_impl,
)