#include "protocol.hpp"

namespace touchcontroller {
namespace protocol {

bool deserialize_event(ProxyMessage& message, const std::vector<uint8_t> data) {
    const uint8_t* ptr = data.data();
    const uint8_t* end = data.data() + data.size();

    if (end - ptr < sizeof(uint32_t)) {
        std::cerr << "Not enough data to read message type" << std::endl;
        return false;
    }

    uint32_t raw_type = *reinterpret_cast<const uint32_t*>(ptr);
    ptr += sizeof(uint32_t);
    ProxyMessage::Type type = static_cast<ProxyMessage::Type>(ntohl(raw_type));

    switch (type) {
        case ProxyMessage::Initialize: {
            message.type = type;
            return true;
        }
        case ProxyMessage::Vibrate: {
            message.type = type;
            if (end - ptr < sizeof(uint32_t)) {
                std::cerr << "Not enough data to read vibrate kind" << std::endl;
                return false;
            }
            uint32_t vibrate_kind = ntohl(*reinterpret_cast<const uint32_t*>(ptr));
            message.vibrate.kind = static_cast<VibrateKind>(static_cast<int32_t>(vibrate_kind));
            return true;
        }
        case ProxyMessage::InputStatus: {
            message.type = type;
            if (end - ptr < sizeof(uint8_t)) {
                std::cerr << "Not enough data to read has status flag" << std::endl;
                return false;
            }
            message.input_status.has_status = *ptr++ != 0;
            if (message.input_status.has_status) { 
                if (end - ptr < sizeof(uint32_t)) {
                    std::cerr << "Not enough data to read text length" << std::endl;
                    return false;
                }

                uint32_t text_length = ntohl(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                if (end - ptr < text_length) {
                    std::cerr << "Not enough data to read text" << std::endl;
                    return false;
                }
                char* buffer = reinterpret_cast<char*>(malloc(text_length + 1));
                memcpy(buffer, ptr, text_length);
                buffer[text_length] = '\0';
                ptr += text_length;
                message.input_status.text = buffer;

                if (end - ptr < sizeof(uint32_t) * 4) {
                    std::cerr << "Not enough data to read length data" << std::endl;
                    return false;
                }
                message.input_status.composition_start = ntohl(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_status.composition_length = ntohl(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_status.selection_start = ntohl(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_status.selection_length = ntohl(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);

                if (end - ptr < sizeof(uint8_t)) {
                    std::cerr << "Not enough data to read selection left flag" << std::endl;
                    return false;
                }
                message.input_status.selection_left = *ptr++ != 0;
            }
            return true;
        }
        case ProxyMessage::KeyboardShow: {
            message.type = type;
            if (end - ptr < sizeof(uint8_t)) {
                std::cerr << "Not enough data to read keyboard show flag" << std::endl;
                return false;
            }
            message.keyboard_show.show = (*ptr++ != 0);
            return true;
        }
        case ProxyMessage::InputCursor: {
            message.type = type;
            if (end - ptr < sizeof(uint8_t)) {
                std::cerr << "Not enough data to read has cursor rect flag" << std::endl;
                return false;
            }
            message.input_cursor.has_cursor_rect = *ptr++ != 0;
            if (message.input_cursor.has_cursor_rect) {
                if (end - ptr < sizeof(uint32_t) * 4) {
                    std::cerr << "Not enough data to read cursor rect" << std::endl;
                    return false;
                }
                message.input_cursor.left = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_cursor.top = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_cursor.width = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_cursor.height = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
            }
            return true;
        }
        case ProxyMessage::InputArea: {
            message.type = type;
            if (end - ptr < sizeof(uint8_t)) {
                std::cerr << "Not enough data to read has area rect flag" << std::endl;
                return false;
            }
            message.input_area.has_area_rect = *ptr++ != 0;
            if (message.input_area.has_area_rect) {
                if (end - ptr < sizeof(uint32_t) * 4) {
                    std::cerr << "Not enough data to read area rect" << std::endl;
                    return false;
                }
                message.input_area.left = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_area.top = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_area.width = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
                message.input_area.height = ntohf(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += sizeof(uint32_t);
            }
            return true;
        }
        default: {
            return false;
        }
    }
}

} // namespace protocol
} // namespace touchcontroller