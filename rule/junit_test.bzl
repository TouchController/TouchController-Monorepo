load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

def _kt_junit_test_impl(name, visibility, srcs, data, test_class, deps, runtime_deps, jvm_flags):
    kt_jvm_test(
        name = name,
        visibility = visibility,
        srcs = srcs,
        data = data,
        args = ["execute", "--select-class", test_class],
        main_class = "org.junit.platform.console.ConsoleLauncher",
        runtime_deps = ([] if not runtime_deps else runtime_deps) + [
            "@maven//:org_junit_jupiter_junit_jupiter_engine",
            "@maven//:org_junit_platform_junit_platform_console",
            "@maven//:org_junit_platform_junit_platform_suite_engine",
        ],
        deps = ([] if not deps else deps) + [
            "@maven//:org_jetbrains_kotlin_kotlin_test_junit5",
            "@maven//:org_junit_jupiter_junit_jupiter_api",
            "@maven//:org_junit_platform_junit_platform_suite_api",
        ],
        jvm_flags = jvm_flags,
    )

kt_junit_test = macro(
    implementation = _kt_junit_test_impl,
    attrs = {
        "test_class": attr.string(mandatory = True, configurable = False),
        "deps": attr.label_list(),
        "runtime_deps": attr.label_list(),
        "srcs": attr.label_list(allow_files = True),
        "data": attr.label_list(allow_files = True),
        "jvm_flags": attr.string_list(),
    }
)
