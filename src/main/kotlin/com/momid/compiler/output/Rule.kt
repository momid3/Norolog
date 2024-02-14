package com.momid.compiler.output

class Rule(val unknowns: List<Unknown>, val ifs: List<RuleElement>, then: List<RuleElement>)

/***
 * in "areFriends(A, B) = true":
 *
 * "areFriends(A, B)" -> ruleInfo
 *
 * "=" -> operator
 *
 * "true" -> ruleCondition
 */
class RuleElement(val ruleInfo: RuleInfo, val operator: Operator, val ruleCondition: Evaluation)

/***
 * @param info the actual info.
 * @param givenUnknowns a list of provided "Unknowns" as parameters to the info.
 *
 * in "areFriends(A, B)":
 *
 * "areFriends(Int, Int)" is the defined info.
 *
 * "A" and "B" are the given Unknowns.
 */
class RuleInfo(val info: Info, val givenUnknowns: List<Unknown>)

class Unknown(val name: String)

enum class Operator {
    LessThan, Equal, MoreThan, LessThanOrEqual, MoreThanOrEqual
}
