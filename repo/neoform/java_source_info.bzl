def _java_source_info_init(*, source_jar, deps = [], libraries = []):
    return {
        "source_jar": source_jar,
        "transitive_libraries": depset(
            transitive = [dep.transitive_libraries for dep in deps],
            direct = [library for library in libraries],
        ),
        "libraries": libraries,
    }

JavaSourceInfo, _ = provider(
    doc = "Information about a Java source JAR.",
    fields = {
        "source_jar": "Source JAR file.",
        "transitive_libraries": "depset. Transitive libraries.",
        "libraries": "sequence of files. libraries.",
    },
    init = _java_source_info_init
)
