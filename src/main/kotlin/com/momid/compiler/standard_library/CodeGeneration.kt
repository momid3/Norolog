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

fun createWindow(width: String, height: String): String {
    return "SDL_CreateWindow(\"Norolog\", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, " + width + ", " + height + ", SDL_WINDOW_RESIZABLE)"
}

fun sleep(seconds: String): String {
    return "sleep(" + seconds + ")"
}

fun createRenderer(window: String): String {
    return "SDL_CreateRenderer(" + window + ", -1, SDL_RENDERER_ACCELERATED)"
}

fun drawLine(renderer: String, x0: String, y0: String, x1: String, y1: String): String {
    return "SDL_RenderDrawLine(" + renderer + ", " + x0 + ", " + y0 + ", " + x1 + ", " + y1 + ")"
}

fun update(renderer: String): String {
    return "SDL_RenderPresent(" + renderer + ")"
}

fun setRendererColor(renderer: String, a: String, r: String, g: String, b: String): String {
    return "SDL_SetRenderDrawColor(" + renderer + ", " + r + ", " + g + ", " + b + ", " + a + ")"
}
