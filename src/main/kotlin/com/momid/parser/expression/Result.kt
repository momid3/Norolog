package com.momid.parser.expression

open class Result<T>

open class Ok<T>(val ok: T): Result<T>()

open class Error<T>(val error: String, val range: IntRange): Result<T>()


fun <T, R> Error<T>.to(): Error<R> {
    return Error(this.error, this.range)
}