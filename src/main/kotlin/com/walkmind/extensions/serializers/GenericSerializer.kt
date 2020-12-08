package com.walkmind.extensions.serializers

interface Encoder<in T, R> {
    fun encode(value: T): R
}

interface Decoder<out T, R> {
    fun decode(input: R): T
}

interface Serializer<T, R> : Encoder<T, R>, Decoder<T, R>