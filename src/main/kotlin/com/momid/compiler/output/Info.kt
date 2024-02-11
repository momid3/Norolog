package com.momid.compiler.output

import com.momid.compiler.forEveryIndexed
import com.momid.compiler.typesMatch

class Info(val name: String, val parameters: List<InfoParameter>, val valueType: OutputType, val declarationPackage: String = "") {
    override fun equals(other: Any?): Boolean {
        return other is Info && this.name == other.name && this.parameters.forEveryIndexed { index, parameter ->
            parameter == other.parameters[index]
        } && this.declarationPackage == other.declarationPackage
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + valueType.hashCode()
        result = 31 * result + declarationPackage.hashCode()
        return result
    }
}

class InfoParameter(val type: OutputType) {
    override fun equals(other: Any?): Boolean {
        return other is InfoParameter && typesMatch(other.type, this.type).first
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}

/***
 * Map <Info, Pair<Corresponding C Struct, the C Struct instance variable>>
 */
class InfosInformation(val infosInformation: HashMap<Info, InfoInformation>)

class InfoInformation(
    val cStruct: CStruct,
    val cListInstanceVariableName: String,
    val listCurrentIndexVariableName: String,
    val listSizeVariableName: String
)
