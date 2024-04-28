# Norolog
Compiler for an interesting programming langauge.

What Is Norolog:

the word norolog is a combination of neuro + logic. it was originally meant to be a DSL specifically for neuro-symbolic logical applications (as name suggests). but i figured out for it to be fast i want it to provide enough low level functionality. so for now, it's a systems programming language.

specifications:

type system: norolog is statically typed.
backend: it binds to C as backend.
memory management: norolog will not have a GC. it will have a system to handle memory management at compile time. similar to rust, but with none of the rust borrowing rules.
other features: genrics, functional programming (lambdas and closure), methods, type inference, extension functions, working with references directly (but safely), nullability- mutability-errorability of types.
GPU programming: norolog provides first-class support for general purpose gpu programming. currently via Nvidia's CUDA. (it's partly implemented but is in the working!)

here is what norolog looks like:

```kotlin
class <W> ArrayList(items: [[W, 3], 3])

// * means pass by reference. it will automatically referene and dereference it
fun show(parameter: *Int, otherParameter: (Int) -> Int) {
    parameter = 333;
    otherParameter.invoke(3);
}

// kotlin-style also function
fun <T> T.also(block: (T) -> Nor): T {
    block.invoke(this);
    return this
}

fun main() {
    val keep = Keep(3); // type is Keep<T> is inferred
    val change = 3;
    val some = [[3, 3], 3];

    print(some[0][0]);

    change.also({ it ->
        print(it);
    });

    print("change is ");
    print(change);

    print(#(abs(#change), Int)); // interoperability with C (calling C's abs function)

    initGraphics();
    val window = window(700, 700);
    window.setColor(170, 170, 170, 255);

    for (w in 0 until 700) {
        for(h in 0 until 700) {
            val pixelValue = sqrt(w * w)+(#h * h);
            window.setColor(pixelValue, pixelValue, pixelValue, 255);
            window.pixel(w, h);
        }
    }

    window.update();
    graphicsSleep(333);
}

class <R> Keep(
    keepValue: R
)

```
