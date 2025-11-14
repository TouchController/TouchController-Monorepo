def parse_pin_file(content):
    lines = content.split("\n")
    hashes = {}
    for line in lines:
        space_index = line.find(" ")
        url = line[:space_index]
        hash = line[space_index + 1:]
        hashes[url] = hash
    return hashes