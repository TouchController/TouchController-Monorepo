#pragma once
#include <cstdint>
#include <vector>
#include <iostream>
#include <string>
#include <cstring>

#ifdef WIN32
#include <winsock2.h>
#else
#include <arpa/inet.h>
#endif

#if defined(__MINGW32__) || not(defined(WIN32)) || (defined(WINVER) && WINVER < 0x0602)
static uint32_t htonf(float value) {
    uint32_t int_value = *(uint32_t*)(&value);
    return htonl(int_value);
}
static float ntohf(uint32_t value) {
    uint32_t int_value = ntohl(value);
    return *(float*)(&int_value);
}
#endif

struct InputStatusData {
    bool has_status;
    const char* text;
    int composition_start;
    int composition_length;
    int selection_start;
    int selection_length;
    bool selection_left;
};

enum VibrateKind {
    UNKNOWN = -1,
    BLOCK_BROKEN = 0,
};

struct ProxyMessage {
    enum Type : uint32_t {
        Add = 1,
        Remove = 2,
        Clear = 3,
        Vibrate = 4,
        Capability = 5,
        Large = 6,
        InputStatus = 7,
        KeyboardShow = 8,
        InputCursor = 9,
        Initialize = 10,
        InputArea = 11,
    };

    Type type;

    union {
        struct {
            uint32_t index;
            float x;
            float y;
        } add;

        struct {
            uint32_t index;
        } remove;

        struct {
            VibrateKind kind;
        } vibrate;

        struct {
            char name[256];
            bool enabled;
        } capability;

        struct {
            uint8_t length;
            bool end;
            uint8_t payload[240];
        } large;

        struct InputStatusData input_status;

        struct {
            bool show;
        } keyboard_show;

        struct {
            bool has_cursor_rect;
            float left;
            float top;
            float width;
            float height;
        } input_cursor;

        struct {
            bool has_area_rect;
            float left;
            float top;
            float width;
            float height;
        } input_area;
    };

    void serialize(std::vector<uint8_t>& buffer) const {
        buffer.clear();

        uint32_t msg_type = htonl(static_cast<uint32_t>(type));
        append(buffer, msg_type);

        switch (type) {
            case Add: {
                uint32_t pointer_index = htonl(add.index);
                append(buffer, pointer_index);
                append(buffer, htonf(add.x));
                append(buffer, htonf(add.y));
                break;
            }
            case Remove: {
                uint32_t pointer_index = htonl(remove.index);
                append(buffer, pointer_index);
                break;
            }
            case Clear:
            case Initialize:
                break;
            case Vibrate: {
                uint32_t kind = htonl(static_cast<uint32_t>(static_cast<int32_t>(vibrate.kind)));
                append(buffer, kind);
                break;
            }
            case Large: {
                append(buffer, large.length);
                buffer.insert(buffer.end(), large.payload,
                              large.payload + large.length);
                buffer.push_back(large.end ? 1 : 0);
                break;
            }
            case Capability: {
                uint8_t str_length =
                    static_cast<uint8_t>(strlen(capability.name));
                buffer.push_back(str_length);
                buffer.insert(buffer.end(), capability.name, capability.name + str_length);
                buffer.push_back(capability.enabled ? 1 : 0);
                break;
            }
            case InputStatus: {
                if (input_status.has_status) {
                    buffer.push_back(1);
                    uint32_t text_length = static_cast<uint32_t>(strlen(input_status.text));
                    append(buffer, htonl(text_length));
                    buffer.insert(buffer.end(), input_status.text,
                                  input_status.text + text_length);
                    std::free((void*) input_status.text);
                    append(buffer, htonl(input_status.composition_start));
                    append(buffer, htonl(input_status.composition_length));
                    append(buffer, htonl(input_status.selection_start));
                    append(buffer, htonl(input_status.selection_length));
                    buffer.push_back(input_status.selection_left ? 1 : 0);
                } else {
                    buffer.push_back(0);
                }
                break;
            }
            case KeyboardShow: {
                append(buffer, keyboard_show.show ? 1 : 0);
                break;
            }
            case InputCursor: {
                buffer.push_back(input_cursor.has_cursor_rect ? 1 : 0);
                if (input_cursor.has_cursor_rect) {
                    append(buffer, htonf(input_cursor.left));
                    append(buffer, htonf(input_cursor.top));
                    append(buffer, htonf(input_cursor.width));
                    append(buffer, htonf(input_cursor.height));
                }
                break;
            }
            case InputArea: {
                buffer.push_back(input_area.has_area_rect ? 1 : 0);
                if (input_area.has_area_rect) {
                    append(buffer, htonf(input_area.left));
                    append(buffer, htonf(input_area.top));
                    append(buffer, htonf(input_area.width));
                    append(buffer, htonf(input_area.height));
                }
                break;
            }
        }
    }

   private:
    template <typename T>
    static void append(std::vector<uint8_t>& buffer, const T& value) {
        const uint8_t* p = reinterpret_cast<const uint8_t*>(&value);
        buffer.insert(buffer.end(), p, p + sizeof(T));
    }
};

namespace touchcontroller {
namespace protocol {

bool deserialize_event(ProxyMessage& message, const std::vector<uint8_t> data);
    
}
}