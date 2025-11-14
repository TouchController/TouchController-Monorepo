def camel_case_to_snake_case(camel_str):
    result = []
    for i in range(len(camel_str)):
        char = camel_str[i]
        if char.isupper():
            if i > 0:
                result.append('_')
            result.append(char.lower())
        else:
            result.append(char)
    return ''.join(result)
