#include "event.hpp"

static std::mutex g_event_queue_mutex;
static std::deque<ProxyMessage> g_event_queue;

namespace touchcontroller {
namespace event {

void push_event(ProxyMessage message) {
    std::lock_guard<std::mutex> lock(g_event_queue_mutex);

    g_event_queue.push_back(message);
}

std::optional<std::vector<uint8_t>> poll_event() {
    std::lock_guard<std::mutex> lock(g_event_queue_mutex);

    if (g_event_queue.empty()) {
        return std::nullopt;
    }

    ProxyMessage message = g_event_queue.front();
    g_event_queue.pop_front();

    std::vector<uint8_t> msg_buffer;
    message.serialize(msg_buffer);
    return msg_buffer;
}

}  // namespace event
}  // namespace touchcontroller