#include "wayland.hpp"

#include <wayland-client-core.h>
#include <wayland-client-protocol.h>
#include <wayland-client.h>
#include <wayland-util.h>

#include <iostream>
#include <memory>
#include <string>
#include <unordered_map>

#include "event.hpp"
#include "protocol.hpp"

namespace {

uint32_t surface_width;
uint32_t surface_height;
std::mutex g_next_pointer_id_mutex;
uint32_t next_pointer_id = 1;

class WaylandTouchHandler;
class WaylandSeatHandler;
class WaylandRegistryHandler;
class WaylandSurfaceHandler;

class WaylandTouchHandler {
   private:
    wl_touch *touch;
    wl_surface *surface;
    std::unordered_map<int32_t, uint32_t> pointer_map;

    static void touch_handle_frame(void *data, wl_touch *touch) {
        WaylandTouchHandler *self =
            reinterpret_cast<WaylandTouchHandler *>(data);
        self->handle_frame();
    }
    static void touch_handle_down(void *data, wl_touch *touch, uint32_t serial,
                                  uint32_t time, wl_surface *surface,
                                  int32_t id, wl_fixed_t x, wl_fixed_t y) {
        WaylandTouchHandler *self =
            reinterpret_cast<WaylandTouchHandler *>(data);
        self->handle_down(serial, time, surface, id, x, y);
    }
    static void touch_handle_up(void *data, wl_touch *wl_touch, uint32_t serial,
                                uint32_t time, int32_t id) {
        WaylandTouchHandler *self =
            reinterpret_cast<WaylandTouchHandler *>(data);
        self->handle_up(serial, time, id);
    }
    static void touch_handle_motion(void *data, wl_touch *wl_touch,
                                    uint32_t time, int32_t id, wl_fixed_t x,
                                    wl_fixed_t y) {
        WaylandTouchHandler *self =
            reinterpret_cast<WaylandTouchHandler *>(data);
        self->handle_motion(time, id, x, y);
    };
    static void touch_handle_cancel(void *data, wl_touch *wl_touch) {
        WaylandTouchHandler *self =
            reinterpret_cast<WaylandTouchHandler *>(data);
        self->handle_cancel();
    };
    static uint32_t allocate_new_id() {
        std::lock_guard<std::mutex> pointer_state_lock(g_next_pointer_id_mutex);
        uint32_t next_id = next_pointer_id++;
        return next_id;
    }
    static std::pair<float, float> map_pos(wl_fixed_t x, wl_fixed_t y) {
        float pos_x = wl_fixed_to_double(x) / surface_width;
        float pos_y = wl_fixed_to_double(y) / surface_height;;
        return std::pair(pos_x, pos_y);
    }
    uint32_t map_id(int32_t id) {
        auto mapped_id = pointer_map.find(id);
        if (mapped_id == pointer_map.end()) {
            return 0;
        } else {
            return mapped_id->second;
        }
    }

    void handle_frame() {}
    void handle_down(uint32_t serial, uint32_t time, wl_surface *surface,
                     int32_t id, wl_fixed_t x, wl_fixed_t y) {
        if (surface != this->surface) {
            return;
        }
        uint32_t mapped_id = allocate_new_id();
        std::pair<float, float> pos = map_pos(x, y);
        pointer_map[id] = mapped_id;
        touchcontroller::event::push_event(
            ProxyMessage{ProxyMessage::Type::Add,
                         {.add = {mapped_id, pos.first, pos.second}}});
    }
    void handle_up(uint32_t serial, uint32_t time, int32_t id) {
        uint32_t mapped_id = map_id(id);
        if (mapped_id == 0) {
            return;
        }
        pointer_map.erase(id);
        touchcontroller::event::push_event(
            ProxyMessage{ProxyMessage::Type::Remove, {.remove = {mapped_id}}});
    }
    void handle_motion(uint32_t time, int32_t id, wl_fixed_t x, wl_fixed_t y) {
        uint32_t mapped_id = pointer_map.at(id);
        if (mapped_id == 0) {
            return;
        }
        std::pair<float, float> pos = map_pos(x, y);
        touchcontroller::event::push_event(
            ProxyMessage{ProxyMessage::Type::Add,
                         {.add = {mapped_id, pos.first, pos.second}}});
    }
    void handle_cancel() {
        for (auto it : pointer_map) {
            touchcontroller::event::push_event(ProxyMessage{
                ProxyMessage::Type::Remove, {.remove = {it.second}}});
        }
        pointer_map.clear();
    }

   public:
    WaylandTouchHandler(const WaylandTouchHandler &) = delete;
    WaylandTouchHandler &operator=(const WaylandTouchHandler &) = delete;
    WaylandTouchHandler(wl_touch *touch, wl_surface *surface) {
        this->touch = touch;
        this->surface = surface;
        static const wl_touch_listener touch_listeners = {
            .down = touch_handle_down,
            .up = touch_handle_up,
            .motion = touch_handle_motion,
            .frame = touch_handle_frame,
            .cancel = touch_handle_cancel,
            .shape = nullptr,
            .orientation = nullptr,
        };
        wl_touch_add_listener(touch, &touch_listeners, this);
    }
    ~WaylandTouchHandler() { wl_touch_destroy(touch); }
};

class WaylandSeatHandler {
   private:
    wl_seat *seat;
    wl_surface *surface;
    std::unique_ptr<WaylandTouchHandler> touch;
    std::string name;

    static void seat_handle_capabilities(void *data, wl_seat *seat,
                                         uint32_t capabilities) {
        WaylandSeatHandler *self = reinterpret_cast<WaylandSeatHandler *>(data);
        self->on_capabilities(capabilities);
    }

    static void seat_handle_name(void *data, wl_seat *seat, const char *name) {
        WaylandSeatHandler *self = reinterpret_cast<WaylandSeatHandler *>(data);
        self->on_name(name);
    }

    void on_capabilities(uint32_t capabilities) {
        bool have_touch = capabilities & WL_SEAT_CAPABILITY_TOUCH;
        if (have_touch) {
            wl_touch *touch = wl_seat_get_touch(this->seat);
            this->touch = std::unique_ptr<WaylandTouchHandler>(
                new WaylandTouchHandler(touch, this->surface));
        } else if (!have_touch) {
            this->touch = nullptr;
        }
    }

    void on_name(const std::string &name) { this->name = name; }

   public:
    WaylandSeatHandler(const WaylandSeatHandler &) = delete;
    WaylandSeatHandler &operator=(const WaylandSeatHandler &) = delete;
    WaylandSeatHandler(wl_seat *seat, wl_surface *surface) : touch(nullptr) {
        this->seat = seat;
        this->surface = surface;
        static const wl_seat_listener seat_listeners = {
            .capabilities = seat_handle_capabilities,
            .name = seat_handle_name,
        };
        wl_seat_add_listener(seat, &seat_listeners, this);
    }
    ~WaylandSeatHandler() {
        this->touch = nullptr;
        wl_seat_destroy(seat);
    }
};

class WaylandRegistryHandler {
   private:
    wl_display *display;
    wl_surface *surface;
    wl_registry *registry;
    std::unordered_map<uint32_t, std::unique_ptr<WaylandSeatHandler>>
        seat_handlers;

    static void registry_handle_global(void *data, wl_registry *registry,
                                       uint32_t id, const char *interface,
                                       uint32_t version) {
        WaylandRegistryHandler *self =
            reinterpret_cast<WaylandRegistryHandler *>(data);
        self->handle_global(id, interface, version);
    }

    static void registry_handle_global_remove(void *data, wl_registry *registry,
                                              uint32_t id) {
        WaylandRegistryHandler *self =
            reinterpret_cast<WaylandRegistryHandler *>(data);
        self->seat_handlers.erase(id);
    }

    void handle_global(uint32_t id, const std::string &interface,
                       uint32_t version) {
        std::string interface_name = interface;
        if (interface_name == "wl_seat" && version >= 7) {
            wl_seat *seat = reinterpret_cast<wl_seat *>(
                wl_registry_bind(registry, id, &wl_seat_interface, 7));
            std::unique_ptr<WaylandSeatHandler> handler(
                new WaylandSeatHandler(seat, this->surface));
            this->seat_handlers.insert(std::pair(id, std::move(handler)));
        }
    }

   public:
    WaylandRegistryHandler(const WaylandRegistryHandler &ptr) = delete;
    WaylandRegistryHandler &operator=(const WaylandRegistryHandler &ptr) =
        delete;
    WaylandRegistryHandler(wl_display *display, wl_surface *surface) {
        this->display = display;
        this->surface = surface;
        this->registry = wl_display_get_registry(display);
        static const wl_registry_listener registry_listener = {
            .global = registry_handle_global,
            .global_remove = registry_handle_global_remove,
        };
        wl_registry_add_listener(this->registry, &registry_listener, this);
        wl_display_roundtrip(display);
    }

    ~WaylandRegistryHandler() { wl_registry_destroy(registry); }
};

void init(wl_display *display, wl_surface *surface) {
    new WaylandRegistryHandler(display, surface);
    return;
}
}  // namespace

extern "C" {
JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_platform_wayland_Interface_init(
    JNIEnv *env, jclass clazz, jlong display_handle, jlong surface_handle) {
    wl_display *display = reinterpret_cast<wl_display *>(display_handle);
    wl_surface *surface = reinterpret_cast<wl_surface *>(surface_handle);
    init(display, surface);
}

JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_platform_wayland_Interface_resize(
    JNIEnv *env, jclass clazz, jint width, jint height) {
    surface_width = static_cast<uint32_t>(width);
    surface_height = static_cast<uint32_t>(height);
}

JNIEXPORT jint JNICALL
Java_top_fifthlight_touchcontroller_platform_wayland_Interface_pollEvent(
    JNIEnv *env, jclass clazz, jbyteArray buffer) {
    std::optional<std::vector<uint8_t>> event =
        touchcontroller::event::poll_event();
    if (!event.has_value()) {
        return 0;
    }

    std::vector<uint8_t> msg_buffer = event.value();

    jsize buffer_length = env->GetArrayLength(buffer);
    if (msg_buffer.size() > static_cast<size_t>(buffer_length)) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Buffer overflow");
        return 0;
    }

    env->SetByteArrayRegion(buffer, 0, static_cast<jsize>(msg_buffer.size()),
                            reinterpret_cast<const jbyte *>(msg_buffer.data()));
    return static_cast<jint>(msg_buffer.size());
}
}
