package com.walkmind.extensions.bytebuf

import io.netty.buffer.ByteBuf

fun ByteBuf.moveRemainsToHead() {
    if (this.readableBytes() != this.capacity()) {
        val ret = this.slice()
        this.readerIndex(0)
        this.writerIndex(0)

        // When data in the buffer occupies more than half of capacity we'll copy it in two chunks to avoid overriding the overlap.
        if (this.readableBytes() > this.capacity() / 2)
            ret.readBytes(this, this.capacity() / 2)

        ret.readBytes(this, ret.readableBytes())
    }
}

inline fun <R> ByteBuf.use(block: (ByteBuf) -> R): R {
    try {
        return block(this)
    } catch (e: Throwable) {
        throw e
    } finally {
        this.release()
    }
}
