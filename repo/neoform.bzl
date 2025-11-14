"""NeoForm repository rule, fetches NeoForm artifact and setup Bazel rules"""

load("@//private:maven_coordinate.bzl", _convert_maven_coordinate = "convert_maven_coordinate", _convert_maven_coordinate_to_repo = "convert_maven_coordinate_to_repo", _convert_maven_coordinate_to_url = "convert_maven_coordinate_to_url")
load("@//private:snake_case.bzl", _camel_case_to_snake_case = "camel_case_to_snake_case")
load("@//private:pin_file.bzl", _parse_pin_file = "parse_pin_file")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_jar")

_neoform_repository_url = "https://maven.neoforged.net/releases"

def _neoform_repo_impl(rctx):
    pin_content = {}
    if rctx.attr.pin_file != None:
        pin_content = _parse_pin_file(rctx.read(rctx.attr.pin_file))

    version_name = rctx.attr.version
    version_sha256 = rctx.attr.sha256
    neoform_zip = "neoform.zip"
    output_prefix = "neoform/%s" % version_name

    rctx.report_progress("Downloading NeoForm JAR %s" % version_name)
    rctx.download(
        url = "%s/net/neoforged/neoform/%s/neoform-%s.zip" % (_neoform_repository_url, version_name, version_name),
        sha256 = version_sha256,
        output = neoform_zip,
    )
    rctx.extract(
        archive = neoform_zip,
        output = output_prefix,
    )

    config_data = json.decode(rctx.read("%s/config.json" % output_prefix))

    mapping_path = config_data["data"]["mappings"]
    rctx.symlink("%s/%s" % (output_prefix, mapping_path), "mappings.tsrg")
    inject_path = config_data["data"]["inject"]
    rctx.symlink("%s/%s" % (output_prefix, inject_path), "inject")
    root_build_content = [
        'package(default_visibility = ["//visibility:public"])',
        "",
        "alias(",
        '    name = "mappings",',
        '    actual = "mappings.tsrg",',
        ")",
        "alias(",
        '    name = "injects",',
        '    actual = ":inject",',
        ")",
        "alias(",
        '    name = "neoform",',
        '    actual = "%s",' % neoform_zip,
        ")",
    ]

    rctx.file(
        "BUILD.bazel",
        "\n".join(root_build_content),
    )

    rctx.report_progress("Downloading function JARs for NeoForm %s" % version_name)
    function_jar_tokens = {}
    for function_name in config_data["functions"]:
        function = config_data["functions"][function_name]
        version = function["version"]

        token = rctx.download(
            url = _convert_maven_coordinate_to_url(_neoform_repository_url, version),
            output = "function_jars/%s.jar" % function_name,
            sha256 = pin_content.get(_convert_maven_coordinate_to_url(_neoform_repository_url, version), ""),
            block = False,
        )
        function_jar_tokens[function_name] = token

    rctx.file("functions/BUILD.bazel", "")
    for function_name in config_data["functions"]:
        function = config_data["functions"][function_name]
        version = function["version"]

        token = function_jar_tokens[function_name]
        token.wait()
        rctx.extract(
            archive = "function_jars/%s.jar" % function_name,
            output = "function_extracted/%s" % function_name,
        )

        manifest = rctx.read("function_extracted/%s/META-INF/MANIFEST.MF" % function_name)
        main_class = None
        lines = manifest.split("\n")
        for line in lines:
            if line.startswith(" "):
                if main_class != None:
                    main_class += line[1:].strip()
                    continue
                else:
                    continue
            elif main_class != None:
                break
            index = line.find(":")
            if index == -1:
                continue
            key = line[0:index].strip()
            value = line[index + 1:].strip()
            if key == "Main-Class":
                main_class = value
        if not main_class:
            fail("Failed to extract main class from manifest")

        jvm_flags = []
        for flag in function["jvmargs"]:
            jvm_flags.append('"%s"' % flag)

        function_build = [
            'load("@rules_java//java:defs.bzl", "java_binary")',
            "",
            "java_binary(",
            '    name = "%s",' % function_name,
            '    visibility = ["//visibility:public"],',
            '    main_class = "%s",' % main_class,
            '    runtime_deps = ["@%s//jar"],' % _convert_maven_coordinate_to_repo("neoform", version),
            "    jvm_flags = [%s]," % ", ".join(jvm_flags),
            ")",
        ]
        rctx.file(
            "functions/%s/BUILD.bazel" % function_name,
            "\n".join(function_build),
        )

        jar_output = function_name != "mergeMappings"

        args = function.get("args", [])
        arg_names = []
        arg_entries = []
        for arg in args:
            type = None
            name = arg
            if arg.startswith("{") and arg.endswith("}"):
                name = arg[1:-1]
                if name == "libraries":
                    type = "jar_list"
                elif name == "version":
                    type = "string"
                elif name == "output":
                    type = "output"
                else:
                    type = "file"
            else:
                type = "plain"
                name = arg
            arg_entries.append({
                "name": name,
                "type": type,
            })
            arg_names.append(name)

        rule_output_extension = ".jar" if function_name != "merge" else ".tsrg"
        rule_impl_name = "_%s_impl" % function_name
        rule_impl = []
        if jar_output:
            rule_impl.append('load("@//repo/neoform:java_source_info.bzl", "JavaSourceInfo")')
        rule_impl += [
            "def %s(ctx):" % rule_impl_name,
            '    output_file = ctx.actions.declare_file("_neoform_%s/" + ctx.label.name + "%s")' % (function_name, rule_output_extension),
            "    args = ctx.actions.args()",
            "",
            "    action_inputs = []",
        ]
        if jar_output:
            rule_impl.append("    input_deps = []")
            rule_impl.append("    input_libraries = []")
            if "input" in arg_names:
                rule_impl.append("    if JavaSourceInfo in ctx.attr.input:")
                rule_impl.append("        input_deps.append(ctx.attr.input[JavaSourceInfo])")
        for entry in arg_entries:
            name = entry["name"]
            if entry["type"] == "plain":
                rule_impl.append('    args.add("%s")' % name)
            elif entry["type"] == "file":
                rule_impl.append("    args.add(ctx.file.%s)" % name)
                rule_impl.append("    action_inputs.append(ctx.file.%s)" % name)
            elif entry["type"] == "output":
                rule_impl.append("    args.add(output_file.path)")
            elif entry["type"] == "string":
                rule_impl.append("    args.add(ctx.attr.%s)" % name)
            elif entry["type"] == "jar_list":
                input_depsets_name = "input_%s_depsets" % name
                input_files_name = "input_%s_files" % name
                input_paths_name = "input_%s_paths" % name
                input_paths_path_name = "input_%s_paths_path" % name
                input_paths_file_name = "input_%s_paths_path_file" % name
                rule_impl.append('    %s = "_neoform_%s/" + ctx.label.name + "_%s_libraries.txt"' % (input_paths_path_name, function_name, name))
                rule_impl.append("    %s = []" % input_depsets_name)
                rule_impl.append("    for attr in ctx.attr.%s:" % name)
                rule_impl.append("        %s.append(attr[JavaInfo].compile_jars)" % input_depsets_name)
                rule_impl.append("    %s = depset(transitive = %s).to_list()" % (input_files_name, input_depsets_name))
                if jar_output:
                    rule_impl.append("    input_libraries += %s" % input_files_name)
                rule_impl.append("    %s = []" % input_paths_name)
                rule_impl.append("    for file in %s:" % input_files_name)
                rule_impl.append('        %s.append("-e=" + file.path)' % input_paths_name)
                rule_impl.append("    %s = ctx.actions.declare_file(%s)" % (input_paths_file_name, input_paths_path_name))
                rule_impl.append("    ctx.actions.write(")
                rule_impl.append("        output = %s," % input_paths_file_name)
                rule_impl.append('        content = "\\n".join(%s),' % input_paths_name)
                rule_impl.append("    )")
                rule_impl.append("    args.add(%s.path)" % input_paths_file_name)
                rule_impl.append("    action_inputs += %s" % input_files_name)
                rule_impl.append("    action_inputs += [%s]" % input_paths_file_name)
        rule_impl.append("    ")
        rule_impl.append("    ctx.actions.run(")
        rule_impl.append("        inputs = action_inputs,")
        rule_impl.append("        outputs = [output_file],")
        rule_impl.append("        executable = ctx.executable._%s_bin," % function_name)
        rule_impl.append('        mnemonic = "NeoForm%s",' % function_name.capitalize())
        rule_impl.append('        progress_message = "Running NeoForm function %s to create " + output_file.short_path,' % function_name)
        rule_impl.append("        arguments = [args],")
        rule_impl.append("    )")
        rule_impl.append("    ")
        rule_impl.append("    return [")
        rule_impl.append('        DefaultInfo(files = depset([output_file])),')
        if jar_output:
            rule_impl.append('        JavaSourceInfo(')
            rule_impl.append('            source_jar = output_file,')
            rule_impl.append('            deps = input_deps,')
            rule_impl.append('            libraries = input_libraries,')
            rule_impl.append('        ),')
        rule_impl.append("    ]")
        rule_impl = "\n".join(rule_impl)

        rule_def = [
            "%s = rule(" % function_name,
            "    implementation = %s," % rule_impl_name,
            "    attrs = {",
        ]
        for entry in arg_entries:
            name = entry["name"]
            if entry["type"] == "file":
                rule_def.append('        "%s": attr.label(' % name)
                rule_def.append("            mandatory = True,")
                if jar_output:
                    rule_def.append("            providers = [[], [JavaSourceInfo]],")
                rule_def.append("            allow_single_file = True,")
                rule_def.append("        ),")
            elif entry["type"] == "string":
                rule_def.append('        "%s": attr.string(' % name)
                rule_def.append("            mandatory = True,")
                rule_def.append("        ),")
            elif entry["type"] == "jar_list":
                rule_def.append('        "%s": attr.label_list(' % name)
                rule_def.append("            mandatory = True,")
                rule_def.append("            providers = [JavaInfo],")
                rule_def.append("        ),")
            elif entry["type"] != "output" and entry["type"] != "plain":
                fail("Unsupported argument type: %s" % entry["type"])
        rule_def.append('        "_%s_bin": attr.label(' % function_name)
        rule_def.append('            default = "//functions/%s",' % function_name)
        rule_def.append("            executable = True,")
        rule_def.append('            cfg = "exec",')
        rule_def.append("        )")
        rule_def.append("    },")
        rule_def.append(")")
        rule_def = "\n".join(rule_def)

        rctx.file(
            "functions/%s.bzl" % (function_name),
            rule_impl + "\n" + "\n" + rule_def,
        )

    def convert_task_name(side, name):
        return "%s_%s" % (side, _camel_case_to_snake_case(name))

    rctx.file("tasks/BUILD.bazel", "")
    for side_name in config_data["steps"]:
        steps = config_data["steps"][side_name]
        for step in steps:
            type = step["type"]
            if type.startswith("download") or type == "listLibraries":
                continue

            name = step.get("name", type)
            task_name = convert_task_name(side_name, name)

            task_def = ['package(default_visibility = ["//visibility:public"])']
            if type == "strip":
                task_def.append('load("@//repo/neoform/rule:split_resources.bzl", strip = "split_resources")')
            elif type == "inject":
                task_def.append('load("@//repo/neoform/rule:inject_zip_content.bzl", inject = "inject_zip_content")')
            elif type == "patch":
                task_def.append('load("@//repo/neoform/rule:patch_zip_content.bzl", patch = "patch_zip_content")')
            elif not type.startswith("download") and type != "listLibraries":
                task_def.append('load("//functions:%s.bzl", "%s")' % (type, type))
            task_def.append("")
            task_def.append("%s(" % type)
            task_def.append('    name = "%s",' % task_name)
            if type == "mergeMappings":
                task_def.append('    mappings = "//:mappings",')
            elif type == "inject":
                task_def.append('    deps = ["//:injects"],')
            elif type == "patch":
                task_def.append('    prefix = "%s",' % config_data["data"]["patches"][side_name])
                task_def.append('    patches = "//:neoform",')
            elif type == "split":
                if name == "stripClient":
                    task_def.append("    generate_manifest = True,")
                    task_def.append('    dist_id = "client",')
                    task_def.append('    other_dist_id = "server",')
                    if config_data["steps"]["server"].get("strip", None) != None:
                        task_def.append('    other_dist_jar = "//tasks/server_extract_server",')
                    else:
                        task_def.append('    other_dist_jar = "%s",' % rctx.attr.server_jar)
                    task_def.append('    mappings = "//tasks/client_merge_mappings')
                elif name == "stripServer":
                    task_def.append("    generate_manifest = True,")
                    task_def.append('    dist_id = "server",')
                    task_def.append('    other_dist_id = "client",')
                    task_def.append('    other_dist_jar = "%s",' % rctx.attr.client_jar)
                    task_def.append('    mappings = "//tasks/server_merge_mappings')
            for item_key in step:
                if item_key == "type":
                    continue
                elif item_key == "name":
                    continue
                else:
                    item_value = step[item_key]
                    if item_value.startswith("{") and item_value.endswith("Output}"):
                        output_task = item_value[1:-7]
                        if output_task == "downloadClient":
                            task_def.append('    %s = "%s",' % (item_key, rctx.attr.client_jar))
                        elif output_task == "downloadServer":
                            task_def.append('    %s = "%s",' % (item_key, rctx.attr.server_jar))
                        elif output_task == "downloadClientMappings":
                            task_def.append('    %s = "%s",' % (item_key, rctx.attr.client_mapping))
                        elif output_task == "downloadServerMappings":
                            task_def.append('    %s = "%s",' % (item_key, rctx.attr.server_mapping))
                        elif output_task == "listLibraries":
                            sided_libraries = ['"@%s//jar"' % _convert_maven_coordinate_to_repo("neoform", library) for library in config_data["libraries"][side_name]]
                            sided_libraries = ", ".join(sided_libraries)
                            if side_name == "client":
                                task_def.append('    %s = ["%s", %s],' % (item_key, rctx.attr.client_libraries, sided_libraries))
                            elif side_name == "server":
                                task_def.append('    %s = ["%s", %s],' % (item_key, rctx.attr.server_libraries, sided_libraries))
                            elif side_name == "joined":
                                task_def.append('    %s = ["%s", "%s", %s],' % (item_key, rctx.attr.client_libraries, rctx.attr.server_libraries, sided_libraries))
                            else:
                                fail("Unsupported side when listing libraries: %s" % side_name)
                        else:
                            output_task_name = convert_task_name(side_name, output_task)
                            task_def.append('    %s = "//tasks/%s",' % (item_key, output_task_name))
                    else:
                        task_def.append('    %s = "%s",' % (item_key, item_value))
            task_def.append(")")

            rctx.file(
                "tasks/%s/BUILD.bazel" % task_name,
                "\n".join(task_def),
            )

_neoform_repo = repository_rule(
    implementation = _neoform_repo_impl,
    attrs = {
        "version": attr.string(
            doc = "Version of NeoForm",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "SHA-256 of the NeoForm JAR file",
            mandatory = True,
        ),
        "client_jar": attr.label(
            doc = "Client JAR file",
            allow_single_file = [".jar"],
            mandatory = True,
        ),
        "server_jar": attr.label(
            doc = "Server JAR file",
            allow_single_file = [".jar"],
            mandatory = True,
        ),
        "client_mapping": attr.label(
            doc = "Client mapping file",
            allow_single_file = [".txt"],
            mandatory = True,
        ),
        "server_mapping": attr.label(
            doc = "Server mapping file",
            allow_single_file = [".txt"],
            mandatory = True,
        ),
        "client_libraries": attr.label(
            doc = "Client libraries",
            mandatory = True,
            providers = [JavaInfo],
        ),
        "server_libraries": attr.label(
            doc = "Server libraries",
            mandatory = True,
            providers = [JavaInfo],
        ),
        "pin_file": attr.label(
            doc = "Pin file",
            allow_single_file = [".txt"],
            mandatory = False,
        ),
    },
)

def _neoform_pin_impl(rctx):
    url_lines = []
    for coordinate in rctx.attr.libraries:
        url_lines.append('"%s"' % _convert_maven_coordinate_to_url(_neoform_repository_url, coordinate))
    rctx.template("PinGenerator.java", rctx.attr._pinner_source, {
        "/*INJECT HERE*/": ", ".join(url_lines),
        "/*OUTPUT NAME*/": "neoform_pin"
    })

    build_bazel_contents = [
        'load("@rules_java//java:defs.bzl", "java_binary")',
        'package(default_visibility = ["//visibility:public"])',
        "",
        "java_binary(",
        '    name = "pin",',
        '    srcs = ["PinGenerator.java"],',
        '    main_class = "PinGenerator",',
        ")",
    ]
    rctx.file("BUILD.bazel", "\n".join(build_bazel_contents))

neoform_pin = repository_rule(
    implementation = _neoform_pin_impl,
    attrs = {
        "libraries": attr.string_list(
            doc = "List of library coordinates to pin",
        ),
        "_pinner_source": attr.label(
            allow_single_file = [".java"],
            default = "@//repo/neoform/pin_generator:PinGenerator.java",
        ),
    },
)

version = tag_class(
    attrs = {
        "version": attr.string(
            doc = "Version of NeoForm",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "SHA-256 of the NeoForm JAR file",
            mandatory = True,
        ),
        "client_jar": attr.label(
            doc = "Client JAR file",
            allow_single_file = [".jar"],
            mandatory = True,
        ),
        "server_jar": attr.label(
            doc = "Server JAR file",
            allow_single_file = [".jar"],
            mandatory = True,
        ),
        "client_mapping": attr.label(
            doc = "Client mapping file",
            allow_single_file = [".txt"],
            mandatory = True,
        ),
        "server_mapping": attr.label(
            doc = "Server mapping file",
            allow_single_file = [".txt"],
            mandatory = True,
        ),
        "client_libraries": attr.label(
            doc = "Client libraries",
            mandatory = True,
            providers = [JavaInfo],
        ),
        "server_libraries": attr.label(
            doc = "Server libraries",
            mandatory = True,
            providers = [JavaInfo],
        ),
    },
)

pin = tag_class(
    attrs = {
        "pin_file": attr.label(
            doc = "Pin file",
            allow_single_file = [".txt"],
            mandatory = False,
        ),
    },
)

def _neoform_impl(mctx):
    versions = {}
    pin_file = None
    for module in mctx.modules:
        for pin in module.tags.pin:
            if pin_file != None:
                fail("Multiple pins found")
            else:
                pin_file = pin.pin_file
        for version in module.tags.version:
            if version in versions:
                if versions[version.version].sha256 != version.sha256:
                    fail("NeoForm version %s already exists with a different SHA-256" % version.version)
                elif versions[version.version].client_jar != version.client_jar:
                    fail("NeoForm version %s already exists with a different client JAR" % version.version)
                elif versions[version.version].server_jar != version.server_jar:
                    fail("NeoForm version %s already exists with a different server JAR" % version.version)
                elif versions[version.version].client_mapping != version.client_mapping:
                    fail("NeoForm version %s already exists with a different client mapping" % version.version)
                elif versions[version.version].server_mapping != version.server_mapping:
                    fail("NeoForm version %s already exists with a different server mapping" % version.version)
                elif versions[version.version].client_libraries != version.client_libraries:
                    fail("NeoForm version %s already exists with a different client libraries" % version.version)
                elif versions[version.version].server_libraries != version.server_libraries:
                    fail("NeoForm version %s already exists with a different server libraries" % version.version)
            else:
                versions[version.version] = {
                    "version": version.version,
                    "sha256": version.sha256,
                    "client_jar": version.client_jar,
                    "server_jar": version.server_jar,
                    "client_mapping": version.client_mapping,
                    "server_mapping": version.server_mapping,
                    "client_libraries": version.client_libraries,
                    "server_libraries": version.server_libraries,
                }
    versions = versions.values()

    libraries = []
    def append_library(coordinate):
        if coordinate not in libraries:
            libraries.append(coordinate)

    for version in versions:
        version_name = version["version"]
        version_sha256 = version["sha256"]
        output_prefix = "neoform/%s" % version_name

        mctx.report_progress("Downloading NeoForm JAR %s" % version_name)
        mctx.download_and_extract(
            url = "%s/net/neoforged/neoform/%s/neoform-%s.zip" % (_neoform_repository_url, version_name, version_name),
            type = "zip",
            sha256 = version_sha256,
            output = output_prefix,
        )

        config_data = json.decode(mctx.read("%s/config.json" % output_prefix))
        for function_name in config_data["functions"]:
            function = config_data["functions"][function_name]
            append_library(function["version"])
        for libraries_side in config_data["libraries"]:
            for library in config_data["libraries"][libraries_side]:
                append_library(library)

        repo_name = _convert_maven_coordinate_to_repo("neoform", version_name)
        _neoform_repo(
            name = repo_name,
            version = version_name,
            sha256 = version_sha256,
            client_jar = version["client_jar"],
            server_jar = version["server_jar"],
            client_mapping = version["client_mapping"],
            server_mapping = version["server_mapping"],
            client_libraries = version["client_libraries"],
            server_libraries = version["server_libraries"],
            pin_file = pin_file,
        )

    pin_content = {}
    if pin_file != None:
        pin_content = _parse_pin_file(mctx.read(pin_file))
    for library in libraries:
        http_jar(
            name = _convert_maven_coordinate_to_repo("neoform", library),
            url = _convert_maven_coordinate_to_url(_neoform_repository_url, library),
            sha256 = pin_content.get(_convert_maven_coordinate_to_url(_neoform_repository_url, library), None),
        )

    neoform_pin(
        name = "neoform_pin",
        libraries = libraries,
    )

neoform = module_extension(
    implementation = _neoform_impl,
    tag_classes = {
        "version": version,
        "pin": pin,
    },
)
