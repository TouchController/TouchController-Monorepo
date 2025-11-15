load("@rules_java//java:defs.bzl", "java_library")
load("@bazel_skylib//rules:expand_template.bzl", "expand_template")

def _neoforge_mod_toml_jar_impl(name, visibility, src, resource_strip_prefix, substitutions):
    expand_template(
        name = name + "_expanded",
        template = src,
        substitutions = substitutions,
        out = "META-INF/neoforge.mods.toml",
    )
    java_library(
        name = name,
        visibility = visibility,
        resources = [name + "_expanded"],
        resource_strip_prefix = resource_strip_prefix,
    )

neoforge_mod_toml_jar = macro(
    attrs = {
        "src": attr.label(
            mandatory = True,
            allow_single_file = [".json"],
            doc = "Input neoforge.mods.toml file",
        ),
        "resource_strip_prefix": attr.string(
            mandatory = True,
        ),
        "substitutions": attr.string_dict(
            mandatory = True,
            doc = "A dictionary mapping strings to their substitutions.",
        ),
    },
    implementation = _neoforge_mod_toml_jar_impl,
)