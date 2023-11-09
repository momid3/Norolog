package com.momid.parser.expression

open class Result<T>

class Ok<T>(val ok: T): Result<T>()

class Error<T>(val error: String, val range: IntRange): Result<T>()
