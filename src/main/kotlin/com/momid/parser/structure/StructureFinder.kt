package com.momid.parser.structure

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

class StructureFinder {

    private val registeredClasses : ArrayList<KClass<*>> = ArrayList()

    fun registerStructures(vararg structures: KClass<*>) {
        registeredClasses.addAll(structures)
    }

    fun start(tokens: List<Char>, sliceShift: Int = 0): List<Structure> {
        var currentTokenIndex = 0
        val foundStructures = ArrayList<Structure>()
        while (true) { whi@
            for (registeredClass in registeredClasses) {
                if (currentTokenIndex >= tokens.size) {
                    break@whi
                }
                val structure = evaluateStructure(registeredClass as KClass<Structure>, currentTokenIndex, tokens) ?: continue
                val structureRange = structure.range
                structure.range = structure.range.shift(sliceShift)
//                    if (currentTokenIndex > tokens.lastIndex) {
//                        continue
//                    } else {
//                        if (structure is Continued) {
//                            structure.continuedStructures = StructureFinder().apply { this.registerStructures(CodeBlock::class) }.start(tokens.slice(structureRange.first..structureRange.last))
//                        }
                val nextTokenIndex = structureRange.last
                foundStructures.add(structure)
                currentTokenIndex = nextTokenIndex
                continue@whi
//                    }
            }

            break
        }

        processStructureFinder(this, foundStructures, tokens)

        return foundStructures
    }
}

fun processStructureFinder(structureFinder: StructureFinder, finderResult: List<Structure>, tokens: List<Char>) {
    finderResult.forEach { structure ->
        if (structure is Continued) {
            val structureRange = structure.range
            structure.continuedStructures = structureFinder
                .start(tokens.slice(structureRange.first..structureRange.last), structureRange.first)
        } else {
            structure::class.memberProperties.forEach { property ->
                if (property is KMutableProperty<*>) {
                    if ((property.returnType.classifier as KClass<*>).isSubclassOf(Continued::class)) {
                        val continued = property.getter.call(structure) as Continued
                        val structureRange = continued.range
                        continued.continuedStructures = structureFinder
                            .start(tokens.slice(structureRange.first..structureRange.last), structureRange.first)
                    }
                }
            }
        }
    }
}

fun Structure.correspondingTokens(tokens: List<Char>): List<Char> {
    val range = this.range
    return tokens.slice(range.first until range.last)
}

fun IntRange.shift(shiftBy: Int): IntRange {
    return IntRange(this.first + shiftBy, this.last + shiftBy)
}
