package top.fifthlight.blazerod.util.cowbuffer

class CowBufferList<C : CowBuffer.Content<C>>(private val list: List<CowBuffer<C>>) : AbstractList<C>() {
    override val size: Int
        get() = list.size

    override fun get(index: Int) = list[index].content
}
