# Norolog
Compiler for an interesting programming langauge.

```kotlin
class SomeClass {
    someVariable: Int,
    anotherVariable: Int,
    someOtherVariable: Int
}

fun someFunction(someParameter: Int): Int {
    return someParameter
}

fun show(parameter: ref Int) {
    print("showing it");
    print(parameter.value);
}

print(someFunction(3));

val theVariable = ref(ref(ref(SomeClass(7, 7, 7))));

val theVal = SomeClass(3, 3, 3);

show(ref(3));

for (someVar in 0 until (theVal.someVariable) * 3) {
    print((theVal.someVariable + 7 + theVariable.value.value.value.someVariable));
}
```
