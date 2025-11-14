"Utils for byte handling"

def _hex_to_bytes(hex_str):
    if len(hex_str) % 2 != 0:
        fail("Hex string length must be even")
    bytes = []
    for i in range(0, len(hex_str), 2):
        hex_pair = hex_str[i] + hex_str[i + 1]
        byte = int(hex_pair, 16)
        bytes.append(byte)
    return bytes

_BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

def _bytes_to_base64(bytes):
    result = []
    padding = 0

    for i in range(0, len(bytes), 3):
        chunk = bytes[i:i + 3]
        if len(chunk) < 3:
            padding = 3 - len(chunk)
            chunk += [0] * padding

        b24 = (chunk[0] << 16) | (chunk[1] << 8) | chunk[2]
        indices = [
            (b24 >> 18) & 0x3F,
            (b24 >> 12) & 0x3F,
            (b24 >> 6) & 0x3F,
            b24 & 0x3F,
        ]
        result += [_BASE64_CHARS[idx] for idx in indices]

    if padding > 0:
        result = result[:-padding] + ["="] * padding
    return "".join(result)

def hex_sha1_to_sri(hex_str):
    bytes = _hex_to_bytes(hex_str)
    if len(bytes) != 20:
        fail("SHA1 must be 20 bytes (40 hex chars)")
    base64_str = _bytes_to_base64(bytes)
    return "sha1-" + base64_str
