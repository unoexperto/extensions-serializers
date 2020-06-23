package com.walkmind.extensions.misc

interface ObjectPool<T> {
    fun acquire(): T
    fun release(obj: T)
}

inline fun <T, R> ObjectPool<T>.use(body: (T) -> R): R {
    var obj: T? = null
    try {
        obj = this.acquire()
        return body(obj)
    } finally {
        if (obj != null)
            this.release(obj)
    }
}
