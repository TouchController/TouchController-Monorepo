// input.hpp
#pragma once
#include <deque>
#include <mutex>
#include <unordered_map>
#include <vector>
#include "protocol.hpp"
#include <windows.h>

struct InitializeError : public std::runtime_error {
	using std::runtime_error::runtime_error;
};

struct EventError : public std::runtime_error {
	using std::runtime_error::runtime_error;
};

extern std::mutex g_event_queue_mutex;
extern std::deque<ProxyMessage> g_event_queue;

void init(HWND handle);
