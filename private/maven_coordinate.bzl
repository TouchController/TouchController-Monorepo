def convert_maven_coordinate(maven_coordinate):
    return maven_coordinate.replace(":", "_").replace("-", "_").replace(".", "_").replace("+", "_")

def convert_maven_coordinate_to_repo(prefix, maven_coordinate):
    return "%s_%s" % (prefix, convert_maven_coordinate(maven_coordinate))

def convert_maven_coordinate_to_url(repository, maven_coordinate, extension = "jar"):
    parts = maven_coordinate.split(":")
    if len(parts) < 3:
        fail("Invalid maven coordinate: %s" % maven_coordinate)
    group = parts[0]
    artifact = parts[1]
    version = parts[2]
    classifier = parts[3] if len(parts) > 3 else None
    suffix = "-%s" % classifier if classifier else ""
    return "%s/%s/%s/%s/%s-%s%s.%s" % (
        repository,
        group.replace(".", "/"),
        artifact,
        version,
        artifact,
        version,
        suffix,
        extension
    )
