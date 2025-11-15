MappingInfo = provider(
    fields = ["format", "namespace_mappings", "source_namespace", "file"],
)

def _merge_mapping_input_impl(ctx):
    valid_formats = ["tiny", "tinyv2", "proguard"]
    if ctx.attr.format not in valid_formats:
        fail("Invalid format: {}. Valid formats are: {}".format(
            ctx.attr.format,
            ", ".join(valid_formats),
        ))

    return [MappingInfo(
        format = ctx.attr.format,
        namespace_mappings = ctx.attr.namespace_mappings,
        source_namespace = ctx.attr.source_namespace,
        file = ctx.file.file,
    )]

merge_mapping_input = rule(
    implementation = _merge_mapping_input_impl,
    attrs = {
        "file": attr.label(
            allow_single_file = [".tiny", ".txt"],
            mandatory = True,
            doc = "Input mapping file",
        ),
        "format": attr.string(
            mandatory = True,
            doc = "Mapping format (tiny / tinyv2 / proguard)",
        ),
        "source_namespace": attr.string(
            mandatory = False,
            doc = "Source namespace",
        ),
        "namespace_mappings": attr.string_dict(
            default = {},
            doc = "Namespace mappings for this input file",
        ),
    },
    doc = "Defines a single mapping file input with its parameters",
)

def _merge_mapping_impl(ctx):
    output_file = ctx.actions.declare_file(ctx.attr.output)

    args = ctx.actions.args()
    inputs = []

    for from_ns, to_ns in ctx.attr.complete_namespace.items():
        args.add("--complete_namespace", "{}:{}".format(from_ns, to_ns))

    for target in ctx.attr.inputs:
        info = target[MappingInfo]
        args.add("--format", info.format)
        if info.source_namespace:
            args.add("--source-namespace", info.source_namespace)
        for from_ns, to_ns in info.namespace_mappings.items():
            args.add("--namespace-mapping", "{}:{}".format(from_ns, to_ns))
        args.add(info.file.path)
        inputs.append(info.file)

    args.add(output_file.path)

    ctx.actions.run(
        inputs = inputs,
        outputs = [output_file],
        executable = ctx.executable._mapping_merger,
        arguments = [args],
        progress_message = "Merging mapping files to %s" % output_file.short_path,
    )

    target = {
        "DefaultInfo": DefaultInfo(files = depset([output_file])),
        "MappingInfo": MappingInfo(
            format = "tinyv2",
            namespace_mappings = {},
            source_namespace = ctx.attr.output_source_namespace,
            file = output_file,
        ),
    }

    return target.values()

merge_mapping = rule(
    implementation = _merge_mapping_impl,
    attrs = {
        "inputs": attr.label_list(
            providers = [MappingInfo],
            mandatory = True,
            doc = "List of mapping inputs to merge",
        ),
        "output": attr.string(
            mandatory = True,
            doc = "Output file name",
        ),
        "complete_namespace": attr.string_dict(
            default = {},
            doc = "Complete missing for namespace",
        ),
        "output_source_namespace": attr.string(
            mandatory = False,
            doc = """Output source namespace. This is not used in remapping,
just a metadata of output which is used as other tasks' input""",
        ),
        "_mapping_merger": attr.label(
            default = Label("//rule/mapping_merger"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Merges multiple mapping files into a single mapping",
)
