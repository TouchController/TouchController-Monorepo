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

#if defined(__MINGW32__) || not(defined(WIN32))
static uint32_t htonf(float value) {
    uint32_t int_value = *(uint32_t*)(&value);
    return htonl(int_value);
}
#endif

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
            char name[256];
        } capability;

        struct {
            uint8_t length;
            bool end;
            uint8_t payload[240];
        } large;

        struct {
            bool has_status;
            const char* text;
            int composition_start;
            int composition_length;
            int selection_start;
            int selection_length;
            uint8_t selection_left;
        } input_status;

        struct {
            bool show;
        } keyboard_show;
    };

    bool is_large_packet() const {
        switch (type) {
            case InputStatus:
                return true;
            default:
                return false;
        }
    };

    void serialize(std::vector<uint8_t>& buffer) const {
        buffer.clear();

        // 写入类型
        uint32_t msg_type = htonl(static_cast<uint32_t>(type));
        append(buffer, msg_type);

        // 根据类型写入数据
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
            case Vibrate:
                break;
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
                break;
            }
            case InputStatus: {
                if (input_status.has_status) {
                    buffer.push_back(1);
                    uint32_t text_length =
                        static_cast<uint8_t>(strlen(input_status.text));
                    append(buffer, htonl(text_length));
                    buffer.insert(buffer.end(), input_status.text,
                                  input_status.text + text_length);
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
            case KeyboardShow:
                append(buffer, keyboard_show.show ? 1 : 0);
                break;
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