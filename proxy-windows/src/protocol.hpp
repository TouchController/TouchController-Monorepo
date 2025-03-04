#pragma once
#include <cstdint>
#include <vector>
#include <winsock2.h>

#ifdef __MINGW32__
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
		Vibrate = 4
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
		}
	}

private:
	template <typename T>
	static void append(std::vector<uint8_t>& buffer, const T& value) {
		const uint8_t* p = reinterpret_cast<const uint8_t*>(&value);
		buffer.insert(buffer.end(), p, p + sizeof(T));
	}
};
