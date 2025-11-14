load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load(":texture.bzl", "TextureLibraryInfo")

def _kt_texture_source_impl(ctx):
    texture_info = ctx.attr.dep[TextureLibraryInfo]
    output_file = ctx.actions.declare_file(ctx.attr.name + ".kt")

    args = ctx.actions.args()
    args.add("--output")
    args.add(output_file.path)
    args.add("--package")
    args.add(texture_info.package)
    args.add("--class_name")
    args.add(texture_info.class_name)
    for texture in texture_info.textures:
        args.add("--texture")
        args.add(texture.identifier)
        args.add(texture.metadata)
    for texture in texture_info.ninepatch_textures:
        args.add("--ninepatch")
        args.add(texture.identifier)
        args.add(texture.metadata)

    ctx.actions.run(
        inputs = texture_info.files,
        outputs = [output_file],
        executable = ctx.executable._generator_bin,
        arguments = [args],
    )

    return [DefaultInfo(files = depset([output_file]))]

_kt_texture_source = rule(
    implementation = _kt_texture_source_impl,
    attrs = {
        "dep": attr.label(
            providers = [TextureLibraryInfo],
            mandatory = True,
        ),
        "_generator_bin": attr.label(
            default = Label("//rule/combine/kotlin"),
            cfg = "exec",
            executable = True,
        ),
    },
)

def _kt_texture_lib_impl(name, visibility, dep):
    source_lib = name + "_source"
    _kt_texture_source(
        name = source_lib,
        dep = dep,
        tags = ["manual"],
    )

    kt_jvm_library(
        name = name,
        srcs = [source_lib],
        visibility = visibility,
    )

kt_texture_lib = macro(
    implementation = _kt_texture_lib_impl,
    attrs = {
        "dep": attr.label(
            providers = [TextureLibraryInfo],
            mandatory = True,
        ),
    },
)
