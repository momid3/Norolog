package com.momid.compiler.output

import com.momid.compiler.forEveryIndexed

/***
 * class representing the definition of a function.
 * @param name name of the function.
 * @param parameters parameters of the function.
 * @param returnType return type of the function.
 * @param bodyRange index range of the body of the function in the source code.
 */
open class Function(
    var name: String,
    var parameters: List<FunctionParameter>,
    var returnType: OutputType,
    val bodyRange: IntRange
)

/***
 * a function that is applied to a class or another type.
 * such as "someVariable.someFunction()".
 */
class ClassFunction(var receiverType: OutputType, val function: Function):
    Function(function.name, function.parameters, function.returnType, function.bodyRange)

class FunctionParameter(val name: String, val type: OutputType, relatedCFunction: CFunction? = null)

class CFunction(val name: String, val parameters: List<CFunctionParameter>, val returnType: Type, var codeText: String)

class CFunctionParameter(val name: String, val type: Type)

class FunctionsInformation(val functionsInformation: HashMap<Function, CFunction?>)

class GenericFunction(
    val typeParameters: List<GenericTypeParameter>,
    val function: Function,
    var unsubstituted: Boolean = true
) : Function(function.name, function.parameters, function.returnType, function.bodyRange) {

    override fun equals(other: Any?): Boolean {
        return other is GenericFunction && this.name == other.name && this.parameters.forEveryIndexed { index, parameter ->
            parameter.type == other.parameters[index].type
        } && this.returnType == other.returnType && if (this.function is ClassFunction) {
            other.function is ClassFunction && this.function.receiverType == other.function.receiverType
        } else {
            true
        }
    }

    fun clone(): GenericFunction {
        val clonedTypeParameters = typeParameters.map { it.clone() }
        val clonedParameters = parameters.map {
            FunctionParameter(it.name, cloneOutputType(it.type, clonedTypeParameters))
        }
        val clonedReturnType = cloneOutputType(returnType, clonedTypeParameters)
        if (this.function is ClassFunction) {
            val clonedReceiverType = cloneOutputType(this.function.receiverType, clonedTypeParameters)
            return GenericFunction(clonedTypeParameters, ClassFunction(clonedReceiverType, Function(this.name, clonedParameters, clonedReturnType, bodyRange)))
        } else {
            return GenericFunction(clonedTypeParameters, Function(this.name, clonedParameters, clonedReturnType, bodyRange))
        }
    }

    override fun hashCode(): Int {
        var result = typeParameters.hashCode()
        result = 31 * result + function.hashCode()
        return result
    }
}

fun genericFunctionParametersMatch(givenFunctionParameter: FunctionParameter, functionParameter: FunctionParameter): Boolean {
    return if (givenFunctionParameter.type is TypeParameterType) {
        functionParameter.type is TypeParameterType && givenFunctionParameter.type.genericTypeParameter.substitutionType ==
                functionParameter.type.genericTypeParameter.substitutionType
    } else {
        return givenFunctionParameter.type == functionParameter.type
    }
}

fun genericFunctionTypesMatch(givenFunctionType: OutputType, functionType: OutputType): Boolean {
    return if (givenFunctionType is TypeParameterType) {
        functionType is TypeParameterType && givenFunctionType.genericTypeParameter.substitutionType ==
                functionType.genericTypeParameter.substitutionType
    } else {
        return givenFunctionType == functionType
    }
}
