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
#include "wayland-text-input-unstable-v3.h"

namespace {

uint32_t surface_width;
uint32_t surface_height;
std::mutex g_next_pointer_id_mutex;
uint32_t next_pointer_id = 1;

class WaylandTouchHandler;
class WaylandTextInputHandler;
class WaylandSeatHandler;
class WaylandRegistryHandler;

class TextInputStatus {
   public:
    bool has_status;
    std::string text;
    int composition_start;
    int composition_length;
    int selection_start;
    int selection_length;
    bool selection_left;

    TextInputStatus(InputStatusData &data)
        : has_status(data.has_status),
          text(data.text),
          composition_start(data.composition_start),
          composition_length(data.composition_length),
          selection_start(data.selection_start),
          selection_length(data.selection_length),
          selection_left(data.selection_left) {}
};

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
    }
    static void touch_handle_cancel(void *data, wl_touch *wl_touch) {
        WaylandTouchHandler *self =
            reinterpret_cast<WaylandTouchHandler *>(data);
        self->handle_cancel();
    }
    static uint32_t allocate_new_id() {
        std::lock_guard<std::mutex> pointer_state_lock(g_next_pointer_id_mutex);
        uint32_t next_id = next_pointer_id++;
        return next_id;
    }
    static std::pair<float, float> map_pos(wl_fixed_t x, wl_fixed_t y) {
        float pos_x = wl_fixed_to_double(x) / surface_width;
        float pos_y = wl_fixed_to_double(y) / surface_height;
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

class WaylandTextInputHandler {
   private:
    zwp_text_input_v3 *text_input;
    std::unique_ptr<TextInputStatus> status = nullptr;
    bool enabled = false;
    bool focused = false;

    struct PendingEvents {
        std::optional<std::tuple<std::string, int32_t, int32_t>> preedit;
        std::vector<std::string> commits;
        std::vector<std::pair<uint32_t, uint32_t>> deletes;
    };
    PendingEvents pending_events;

    static void text_input_handle_enter(void *data,
                                        zwp_text_input_v3 *text_input,
                                        wl_surface *surface) {
        WaylandTextInputHandler *self =
            reinterpret_cast<WaylandTextInputHandler *>(data);
        self->handle_enter(surface);
    }
    static void text_input_handle_leave(void *data,
                                        zwp_text_input_v3 *text_input,
                                        wl_surface *surface) {
        WaylandTextInputHandler *self =
            reinterpret_cast<WaylandTextInputHandler *>(data);
        self->handle_leave(surface);
    }
    static void text_input_handle_preedit_string(void *data,
                                                 zwp_text_input_v3 *text_input,
                                                 const char *text,
                                                 int32_t cursor_begin,
                                                 int32_t cursor_end) {
        WaylandTextInputHandler *self =
            reinterpret_cast<WaylandTextInputHandler *>(data);
        self->handle_preedit_string(text, cursor_begin, cursor_end);
    }
    static void text_input_handle_commit_string(void *data,
                                                zwp_text_input_v3 *text_input,
                                                const char *text) {
        WaylandTextInputHandler *self =
            reinterpret_cast<WaylandTextInputHandler *>(data);
        self->handle_commit_string(text);
    }
    static void text_input_handle_delete_surrounding_text(
        void *data, zwp_text_input_v3 *text_input, uint32_t before_length,
        uint32_t after_length) {
        WaylandTextInputHandler *self =
            reinterpret_cast<WaylandTextInputHandler *>(data);
        self->handle_delete_surrounding_text(before_length, after_length);
    }
    static void text_input_handle_done(void *data,
                                       zwp_text_input_v3 *text_input,
                                       uint32_t serial) {
        WaylandTextInputHandler *self =
            reinterpret_cast<WaylandTextInputHandler *>(data);
        self->handle_done(serial);
    }

    void handle_enter(wl_surface *surface) {
        focused = true;
        pending_events = PendingEvents{};
        if (enabled && status) {
            zwp_text_input_v3_enable(text_input);
            sync_to_composer(*status);
            zwp_text_input_v3_commit(text_input);
        }
    }
    void handle_leave(wl_surface *surface) {
        focused = false;
        pending_events = PendingEvents{};
        if (!status || !enabled) {
            return;
        }
        zwp_text_input_v3_disable(text_input);
        zwp_text_input_v3_commit(text_input);
        // 去除预编辑文本
        if (status->composition_length == 0) {
            return;
        }
        TextInputStatus new_status = *status;
        new_status.text = status->text.substr(0, status->text.size() - status->composition_length);
        new_status.composition_start = 0;
        new_status.composition_length = 0;
        sync_to_application(new_status);
        this->status = std::make_unique<TextInputStatus>(new_status);
    }
    void handle_preedit_string(const char *text, int32_t cursor_begin, int32_t cursor_end) {
        if (!focused) {
            return;
        }
        
        pending_events.preedit = std::make_tuple(
            text ? std::string(text) : "",
            cursor_begin,
            cursor_end
        );
    }
    void handle_commit_string(const char *text) {
        if (!focused) {
            return;
        }
        
        if (text) {
            pending_events.commits.push_back(std::string(text));
        }
    }
    void handle_delete_surrounding_text(uint32_t before_length, uint32_t after_length) {
        if (!focused) {
            return;
        }
        
        pending_events.deletes.emplace_back(before_length, after_length);
    }
    
    void handle_done(uint32_t serial) {
        if (!focused || !status) {
            pending_events = PendingEvents{};
            return;
        }

        // 创建当前状态的副本用于修改
        TextInputStatus new_status = *status;

        // 步骤0：如果有预编辑文本，则移除选中文本
        if (pending_events.preedit && !std::get<0>(*pending_events.preedit).empty()) {
            new_status.text.erase(new_status.selection_start, new_status.selection_length);
            if (new_status.composition_start >= new_status.selection_start + new_status.selection_length) {
                new_status.composition_start -= new_status.selection_length;
            } else if (new_status.composition_start > new_status.selection_start) {
                new_status.composition_start = new_status.selection_start;
            }
            new_status.selection_length = 0;
        }

        // 步骤1: 移除现有预编辑文本（用光标替换）
        if (new_status.composition_length > 0) {
            std::string& text = new_status.text;
            text = text.substr(0, new_status.composition_start) +
                   text.substr(new_status.composition_start + new_status.composition_length);
            
            // 更新光标位置（原组合文本开始处）
            new_status.selection_start = new_status.composition_start;
            new_status.selection_length = 0;
            
            // 重置组合文本信息
            new_status.composition_start = 0;
            new_status.composition_length = 0;
        }

        // 当前光标位置（用于后续操作）
        size_t cursor_pos = new_status.selection_start;

        // 步骤2: 应用删除操作
        for (const auto& [before, after] : pending_events.deletes) {
            // 计算删除范围
            size_t del_start = (before > cursor_pos) ? 0 : cursor_pos - before;
            size_t del_end = std::min(cursor_pos + after, new_status.text.length());
            
            if (del_end > del_start) {
                // 执行删除
                new_status.text = new_status.text.substr(0, del_start) + 
                                  new_status.text.substr(del_end);
                // 更新光标位置
                cursor_pos = del_start;
            }
        }

        // 步骤3: 应用提交操作
        for (const auto& commit : pending_events.commits) {
            // 在光标位置插入文本
            new_status.text = new_status.text.substr(0, cursor_pos) + 
                              commit + 
                              new_status.text.substr(cursor_pos);
            // 更新光标位置（到插入文本之后）
            cursor_pos += commit.length();
        }

        // 步骤4-6: 应用新的预编辑文本
        if (pending_events.preedit && !std::get<0>(*pending_events.preedit).empty()) {
            const auto& [preedit_text, cursor_begin, cursor_end] = *pending_events.preedit;
            // 插入预编辑文本
            new_status.text = new_status.text.substr(0, cursor_pos) + 
                                preedit_text + 
                                new_status.text.substr(cursor_pos);
            
            // 设置组合文本信息
            new_status.composition_start = cursor_pos;
            new_status.composition_length = preedit_text.length();
        
            // 设置光标/选择位置
            if (cursor_begin == -1 && cursor_end == -1) {
                // 隐藏光标（放在预编辑文本末尾）
                new_status.selection_start = cursor_pos + preedit_text.length();
                new_status.selection_length = 0;
            } else {
                // 计算绝对位置
                size_t abs_begin = cursor_pos + cursor_begin;
                size_t abs_end = cursor_pos + cursor_end;
                
                // 设置选择范围
                if (abs_begin < abs_end) {
                    new_status.selection_start = abs_begin;
                    new_status.selection_length = abs_end - abs_begin;
                    new_status.selection_left = false;
                } else {
                    new_status.selection_start = abs_end;
                    new_status.selection_length = abs_begin - abs_end;
                    new_status.selection_left = true;
                }
            }
        } else {
            // 无预编辑文本
            new_status.composition_start = 0;
            new_status.composition_length = 0;
            new_status.selection_start = cursor_pos;
            // 不改变选择长度
        }

        // 重置挂起事件
        pending_events = PendingEvents{};

        // 同步新状态到应用
        sync_to_application(new_status);
    }

    // 同步文本状态到Java侧
    void sync_to_application(const TextInputStatus &status) {
        char* text_buffer = static_cast<char*>(std::malloc(status.text.size() + 1));
        std::memcpy(text_buffer, status.text.c_str(), status.text.size());
        text_buffer[status.text.size()] = '\0';
        
        touchcontroller::event::push_event(ProxyMessage{
            ProxyMessage::InputStatus,
            {.input_status = {
                .has_status = true,
                .text = text_buffer,
                .composition_start = status.composition_start,
                .composition_length = status.composition_length,
                .selection_start = status.selection_start,
                .selection_length = status.selection_length,
                .selection_left = status.selection_left
            }}
        });

        this->status = std::make_unique<TextInputStatus>(status);
    }

    // 发送文本状态到Wayland合成器
    void sync_to_composer(const TextInputStatus &status) {
        // 移除组合文本后的文本（用于surrounding_text）
        std::string base_text = status.text.substr(0, status.composition_start) +
                              status.text.substr(status.composition_start + status.composition_length);
        
        // 计算光标和锚点位置
        int32_t cursor, anchor;
        if (status.selection_left) {
            cursor = status.selection_start + status.selection_length;
            anchor = status.selection_start;
        } else {
            cursor = status.selection_start;
            anchor = status.selection_start + status.selection_length;
        }
        
        zwp_text_input_v3_set_surrounding_text(
            text_input, 
            base_text.c_str(), 
            cursor, 
            anchor
        );
    }

   public:
    WaylandTextInputHandler(const WaylandTextInputHandler &) = delete;
    WaylandTextInputHandler &operator=(const WaylandTextInputHandler &) =
        delete;
    WaylandTextInputHandler(zwp_text_input_v3 *text_input) {
        this->text_input = text_input;
        static const zwp_text_input_v3_listener text_input_listener = {
            .enter = text_input_handle_enter,
            .leave = text_input_handle_leave,
            .preedit_string = text_input_handle_preedit_string,
            .commit_string = text_input_handle_commit_string,
            .delete_surrounding_text =
                text_input_handle_delete_surrounding_text,
            .done = text_input_handle_done,
        };
        zwp_text_input_v3_add_listener(text_input, &text_input_listener, this);
    }
    ~WaylandTextInputHandler() { zwp_text_input_v3_destroy(text_input); }

    void handle_input_status(const TextInputStatus &status) {
        if (!enabled) {
            zwp_text_input_v3_enable(text_input);
            enabled = true;
        }
        this->status = std::make_unique<TextInputStatus>(status);
        sync_to_composer(status);
        zwp_text_input_v3_commit(text_input);
    }

    void handle_input_cursor(float left, float top, float width, float height) {
        if (!enabled) {
            return;
        }
        zwp_text_input_v3_set_cursor_rectangle(
            text_input, left * surface_width, top * surface_height,
            width * surface_width, height * surface_height);
        zwp_text_input_v3_commit(text_input);
    }

    void show_soft_keyboard() {
        if (!enabled) {
            return;
        }
        zwp_text_input_v3_enable(text_input);
        zwp_text_input_v3_commit(text_input);
    }

    void clear_input_status() {
        if (!enabled) {
            return;
        }
        zwp_text_input_v3_disable(text_input);
        zwp_text_input_v3_commit(text_input);
        enabled = false;
        this->status = nullptr;
    }
};

class WaylandSeatHandler {
   private:
    wl_seat *seat;
    wl_surface *surface;
    std::unique_ptr<WaylandTouchHandler> touch;
    std::string name;
    std::unique_ptr<WaylandTextInputHandler> text_input;

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
        if (this->text_input) {
            this->text_input = nullptr;
        }
        wl_seat_destroy(seat);
    }

    void attach_text_input_manager(zwp_text_input_manager_v3 *manager) {
        if (this->text_input) {
            return;
        }
        zwp_text_input_v3 *text_input =
            zwp_text_input_manager_v3_get_text_input(manager, this->seat);
        this->text_input = std::unique_ptr<WaylandTextInputHandler>(
            new WaylandTextInputHandler(text_input));
    }
    void detach_text_input_manager() {
        if (!this->text_input) {
            return;
        }
        this->text_input = nullptr;
    }

    void handle_input_status(const TextInputStatus &status) {
        this->text_input->handle_input_status(status);
    }

    void handle_input_cursor(float left, float top, float width, float height) {
        this->text_input->handle_input_cursor(left, top, width, height);
    }

    void show_soft_keyboard() {
        this->text_input->show_soft_keyboard();
    }

    void clear_input_status() { this->text_input->clear_input_status(); }
};

class WaylandRegistryHandler {
   private:
    wl_display *display;
    wl_surface *surface;
    wl_registry *registry;
    std::unordered_map<uint32_t, std::unique_ptr<WaylandSeatHandler>>
        seat_handlers;
    zwp_text_input_manager_v3 *text_input_manager = nullptr;
    uint32_t zwp_text_input_manager_id = 0;

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
        if (self->seat_handlers.erase(id)) {
            return;
        } else if (id == self->zwp_text_input_manager_id) {
            for (const auto &entry : self->seat_handlers) {
                entry.second->detach_text_input_manager();
            }
            self->zwp_text_input_manager_id = 0;
        }
    }

    void handle_global(uint32_t id, const std::string &interface,
                       uint32_t version) {
        std::string interface_name = interface;
        if (interface_name == "wl_seat" && version >= 7) {
            wl_seat *seat = reinterpret_cast<wl_seat *>(
                wl_registry_bind(registry, id, &wl_seat_interface, 7));
            std::unique_ptr<WaylandSeatHandler> handler(
                new WaylandSeatHandler(seat, this->surface));
            if (this->text_input_manager) {
                handler->attach_text_input_manager(this->text_input_manager);
            }
            this->seat_handlers.insert(std::pair(id, std::move(handler)));
        } else if (interface_name == "zwp_text_input_manager_v3") {
            this->zwp_text_input_manager_id = id;
            this->text_input_manager =
                reinterpret_cast<zwp_text_input_manager_v3 *>(wl_registry_bind(
                    registry, id, &zwp_text_input_manager_v3_interface, 1));
            for (const auto &entry : this->seat_handlers) {
                entry.second->attach_text_input_manager(
                    this->text_input_manager);
            }
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

    ~WaylandRegistryHandler() {
        if (this->text_input_manager) {
            zwp_text_input_manager_v3_destroy(this->text_input_manager);
        }
        wl_registry_destroy(registry);
    }

    void handle_input_status(const TextInputStatus &status) {
        for (const auto &entry : this->seat_handlers) {
            entry.second->handle_input_status(status);
        }
    }

    void handle_input_cursor(float left, float top, float width, float height) {
        for (const auto &entry : this->seat_handlers) {
            entry.second->handle_input_cursor(left, top, width, height);
        }
    }

    void show_soft_keyboard() {
        for (const auto &entry : this->seat_handlers) {
            entry.second->show_soft_keyboard();
        }
    }

    void clear_input_status() {
        for (const auto &entry : this->seat_handlers) {
            entry.second->clear_input_status();
        }
    }
};

static WaylandRegistryHandler *registry_handler = nullptr;

void init(wl_display *display, wl_surface *surface) {
    if (registry_handler) {
        return;
    }
    registry_handler = new WaylandRegistryHandler(display, surface);
    return;
}
}  // namespace

extern "C" {
JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_init(
    JNIEnv *env, jclass clazz, jlong display_handle, jlong surface_handle) {
    wl_display *display = reinterpret_cast<wl_display *>(display_handle);
    wl_surface *surface = reinterpret_cast<wl_surface *>(surface_handle);
    init(display, surface);
}

JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_resize(
    JNIEnv *env, jclass clazz, jint width, jint height) {
    surface_width = static_cast<uint32_t>(width);
    surface_height = static_cast<uint32_t>(height);
}

JNIEXPORT jint JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_pollEvent(
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

JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_pushEvent(
    JNIEnv *env, jclass clazz, jbyteArray buffer, jint length) {
    std::vector<uint8_t> data;
    data.resize(length);
    env->GetByteArrayRegion(buffer, 0, length,
                            reinterpret_cast<jbyte *>(data.data()));

    ProxyMessage message;
    if (touchcontroller::protocol::deserialize_event(message, data)) {
        switch (message.type) {
            case ProxyMessage::Vibrate: {
                break;
            }
            case ProxyMessage::KeyboardShow: {
                if (message.keyboard_show.show) {
                    registry_handler->show_soft_keyboard();
                }
                break;
            }
            case ProxyMessage::InputStatus: {
                if (message.input_status.has_status) {
                    const TextInputStatus status = message.input_status;
                    if (registry_handler) {
                        registry_handler->handle_input_status(status);
                    }
                    std::free((void *)message.input_status.text);
                } else {
                    if (registry_handler) {
                        registry_handler->clear_input_status();
                    }
                }
                break;
            }
            case ProxyMessage::InputCursor: {
                if (message.input_cursor.has_cursor_rect && registry_handler) {
                    registry_handler->handle_input_cursor(
                        message.input_cursor.left, message.input_cursor.top,
                        message.input_cursor.width,
                        message.input_cursor.height);
                }
                break;
            }
            case ProxyMessage::Initialize: {
                touchcontroller::event::push_event(ProxyMessage{
                    ProxyMessage::Capability, {.capability = {"text_status", true}}});
                touchcontroller::event::push_event(ProxyMessage{
                    ProxyMessage::Capability, {.capability = {"keyboard_show", true}}});
                break;
            }
            default:
                break;
        }
    }
}
}
