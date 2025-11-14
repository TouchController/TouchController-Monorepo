load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

TextureInfo = provider(fields = ["identifier", "metadata", "texture", "background"])
NinePatchTextureInfo = provider(fields = ["identifier", "metadata", "texture"])
TextureGroupInfo = provider(fields = ["textures", "ninepatch_textures", "files"])

def _ninepatch_texture_impl(ctx):
    strip_prefix = ctx.attr.strip_prefix
    if strip_prefix == ".":
        strip_prefix = ctx.label.package

    output_textures = []
    output_files = []
    for src in ctx.files.srcs:
        input_filename = src.basename.split(".")[0]
        metadata_file = ctx.actions.declare_file("%s.json" % input_filename, sibling = src)
        compressed_file = ctx.actions.declare_file("%s.compressed.9.png" % input_filename, sibling = src)
        output_files.append(metadata_file)
        output_files.append(compressed_file)

        if not src.short_path.startswith(strip_prefix):
            fail("Bad strip_prefix: want to strip %s from %s" % (strip_prefix, src.short_path))
        identifier = src.short_path.removeprefix(strip_prefix).replace("/", "_").upper()

        args = ctx.actions.args()
        args.add("ninepatch")
        args.add(src)
        args.add(metadata_file)
        args.add(compressed_file)

        args.use_param_file("@%s", use_always = True)

        ctx.actions.run(
            inputs = [src],
            outputs = [compressed_file, metadata_file],
            executable = ctx.executable._texture_generator,
            execution_requirements = {
                "supports-workers": "1",
                "requires-worker-protocol": "json",
            },
            arguments = [args],
            mnemonic = "CombineTexture",
        )

        output_textures.append(NinePatchTextureInfo(
            identifier = identifier,
            metadata = metadata_file,
            texture = compressed_file,
        ))

    files = depset(output_files)
    return [TextureGroupInfo(
        textures = [],
        ninepatch_textures = output_textures,
        files = files,
    ), DefaultInfo(files = files)]

ninepatch_texture = rule(
    implementation = _ninepatch_texture_impl,
    provides = [TextureGroupInfo],
    attrs = {
        "srcs": attr.label_list(
            mandatory = True,
            doc = "Input files. Must be .9.png files",
            allow_files = [".9.png"],
        ),
        "strip_prefix": attr.string(
            mandatory = False,
            default = ".",
        ),
        "_texture_generator": attr.label(
            default = "//rule/combine/metadata/generator",
            executable = True,
            cfg = "exec",
        ),
    },
)

def _texture_impl(ctx):
    strip_prefix = ctx.attr.strip_prefix
    if strip_prefix == ".":
        strip_prefix = ctx.label.package

    output_textures = []
    output_files = []
    for src in ctx.files.srcs:
        background = ctx.attr.background
        input_filename = src.basename.split(".")[0]
        metadata_file = ctx.actions.declare_file("%s.json" % input_filename, sibling = src)
        output_files.append(src)
        output_files.append(metadata_file)

        if not src.short_path.startswith(strip_prefix):
            fail("Bad strip_prefix: want to strip %s from %s" % (strip_prefix, src.short_path))
        identifier = src.short_path.removeprefix(strip_prefix).replace("/", "_").upper()

        args = ctx.actions.args()
        args.add("texture")
        if background:
            args.add("--background")
        args.add(src)
        args.add(metadata_file)

        args.use_param_file("@%s", use_always = True)

        ctx.actions.run(
            inputs = [src],
            outputs = [metadata_file],
            executable = ctx.executable._texture_generator,
            execution_requirements = {
                "supports-workers": "1",
                "requires-worker-protocol": "json",
            },
            arguments = [args],
            mnemonic = "CombineTexture",
        )

        output_textures.append(TextureInfo(
            identifier = identifier,
            metadata = metadata_file,
            texture = src,
            background = background,
        ))

    files = depset(output_files)
    return [TextureGroupInfo(
        textures = output_textures,
        ninepatch_textures = [],
        files = files,
    ), DefaultInfo(files = files)]

texture = rule(
    implementation = _texture_impl,
    provides = [TextureGroupInfo],
    attrs = {
        "srcs": attr.label_list(
            mandatory = True,
            doc = "Input files. Must be .png files",
            allow_files = [".png"],
        ),
        "background": attr.bool(
            mandatory = False,
            default = False,
            doc = "Specify whether this texture is a background texture. Background texture will not be packed in an altas.",
        ),
        "strip_prefix": attr.string(
            mandatory = False,
            default = ".",
        ),
        "_texture_generator": attr.label(
            default = "//rule/combine/metadata/generator",
            executable = True,
            cfg = "exec",
        ),
    },
)

def _merge_texture_group_info(groups):
    files_depsets = []
    textures = []
    ninepatch_textures = []
    for group in groups:
        textures += group.textures
        ninepatch_textures += group.ninepatch_textures
        files_depsets.append(group.files)
    files = depset(transitive = files_depsets)
    return TextureGroupInfo(
        textures = textures,
        ninepatch_textures = ninepatch_textures,
        files = files,
    )

def _texture_group_impl(ctx):
    groups_infos = [dep[TextureGroupInfo] for dep in ctx.attr.deps]
    merged_group = _merge_texture_group_info(groups_infos)
    return [merged_group, DefaultInfo(files = merged_group.files)]

texture_group = rule(
    implementation = _texture_group_impl,
    provides = [TextureGroupInfo],
    attrs = {
        "deps": attr.label_list(
            providers = [TextureGroupInfo],
            default = [],
            mandatory = False,
        ),
    },
)

TextureLibraryInfo = provider(fields = ["package", "class_name", "textures", "ninepatch_textures", "files"])

def _texture_lib_impl(ctx):
    groups_infos = [dep[TextureGroupInfo] for dep in ctx.attr.deps]
    merged_group = _merge_texture_group_info(groups_infos)
    return [
        TextureLibraryInfo(
            package = ctx.attr.package,
            class_name = ctx.attr.class_name,
            textures = merged_group.textures,
            ninepatch_textures = merged_group.ninepatch_textures,
            files = merged_group.files,
        ),
        DefaultInfo(files = merged_group.files),
    ]

texture_lib = rule(
    implementation = _texture_lib_impl,
    provides = [TextureLibraryInfo],
    attrs = {
        "package": attr.string(
            mandatory = True,
        ),
        "class_name": attr.string(
            mandatory = True,
        ),
        "deps": attr.label_list(
            providers = [TextureGroupInfo],
            default = [],
            mandatory = False,
        ),
    },
)
