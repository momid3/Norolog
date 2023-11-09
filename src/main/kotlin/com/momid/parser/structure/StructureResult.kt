package com.momid.parser.structure

import com.momid.parser.expression.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

abstract class Structure(val template: Template = Template(), var range: IntRange = IntRange.EMPTY)

open class Continued(val continueWithExpression: Expression? = null, var continuedStructures: List<Structure> = emptyList(), val continueCondition: ((KClass<Structure>) -> Boolean)? = null, val classesToRegister: List<KClass<*>> = emptyList()): Structure()

fun <T: Structure> evaluateTemplate(structure: T, template: Template, name: String? = null): Expression {

    if (structure is Continued) {
        if (name != null) {
            return structure.continueWithExpression!!.withName(name)
        } else {
            return structure.continueWithExpression!!
        }
    }

    val clazz = structure::class

    if (template is ExpressionTemplate) {
        if (template.isTemplate()) {
            clazz.declaredMemberProperties.forEach { property ->
                if (property is KMutableProperty<*>) {
                    if (template.templateName == property.name) {
                        val structureClass = property.returnType.classifier as KClass<*>
                        val structureInstance = structureClass.createInstance()
                        return evaluateTemplate(structureInstance as Structure, structureInstance.template.toExpressionTemplateIfExpression(), template.templateName)
                    }
                }
            }
        }
        if (template.isExpression()) {
            if (name != null) {
                return template.templateExpression!![name]
            } else {
                return template.templateExpression!!
            }
        }
    }

    if (template is ExpressionTemplates) {
        val multiExpression = MultiExpression(arrayListOf())
        template.forEach {
            multiExpression.expressions.add(evaluateTemplate(structure, it))
        }
        return multiExpression
    }

    throw(Throwable("unknown template kind"))
}

fun <T: Structure> evaluateStructure(structure: T, expressionResult: ExpressionResult): T {

    val clazz = structure::class
    val instance = clazz.createInstance()
    clazz.declaredMemberProperties.forEach { property ->
        if (property is KMutableProperty<*>) {
            if (expressionResult is MultiExpressionResult) {
                expressionResult.forEach {
                    if (it.expression.name == property.name) {
                        val structureClass = property.returnType.classifier as KClass<*>
                        val structureInstance = structureClass.createInstance()
                        if (instance is Continued) {
//                            property.setter.call(instance, Continued(instance.continueWithExpression, emptyList()))
                        } else {
                            val structureResult = evaluateStructure(structureInstance as Structure, it)
                            property.setter.call(instance, structureResult)
                        }
                    }
                }
            }
        }
    }
    instance.range = expressionResult.range
    return instance
}

inline fun <reified T : Structure> evaluateStructure(startIndex: Int, tokens: List<Char>): T? {
    val instance = T::class.createInstance()
    val template = T::class.memberProperties.find { it.name == "template" } ?: throw(Throwable("the getExpression function of the structure was not found"))
    val expressionResult = evaluateExpressionValueic(evaluateTemplate(instance, (template.get(instance) as Template).toExpressionTemplateIfExpression()), startIndex, tokens)
    if (expressionResult != null) {
        return evaluateStructure(instance, expressionResult)
    } else {
        return null
    }
}

fun <T : Structure> evaluateStructure(structure: KClass<T>, startIndex: Int, tokens: List<Char>): T? {
    val instance = structure.createInstance()
    val template = structure.memberProperties.find { it.name == "template" } ?: throw(Throwable("the getExpression function of the structure was not found"))
    val expressionResult = evaluateExpressionValueic(evaluateTemplate(instance, (template.get(instance) as Template).toExpressionTemplateIfExpression()), startIndex, tokens)
    if (expressionResult != null) {
        return evaluateStructure(instance, expressionResult)
    } else {
        return null
    }
}

class SomeStructure(var hello: String? = null)

class SideStructure() : Structure(
    condition { it != 'a' }
)

class AnStructure(var sideBefore: SideStructure? = null, var sideAfter: SideStructure? = null): Structure(
    "sideBefore"() + "friend" + "sideAfter"()
)

fun main() {
    val structure = SomeStructure("")
    val clazz = structure::class
    val instance = clazz.createInstance()
    clazz.declaredMemberProperties.forEach { property ->
        if (property.name == "hello") {
            if (property is KMutableProperty<*>) {
                property.setter.call(instance, "o")
            }
        }
    }
    println(instance.hello)

    val text = "hello! my friend. how are you today ?"

    val structureResult = evaluateStructure<AnStructure>(7, text.toList())
    println("range of side: " + structureResult?.sideAfter?.range)
}