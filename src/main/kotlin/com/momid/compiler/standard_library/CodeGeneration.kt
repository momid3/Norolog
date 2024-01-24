package com.momid.compiler.standard_library

import com.momid.compiler.cTypeAndVariableName
import com.momid.compiler.output.Type
import com.momid.compiler.variableDeclaration

fun initGraphics(initStatusVariableName: String): String {
    return variableDeclaration(cTypeAndVariableName(Type.Int, initStatusVariableName), "SDL_Init(SDL_INIT_VIDEO)")
}

fun initGraphics(): String {
    return "SDL_Init(SDL_INIT_VIDEO)"
}
