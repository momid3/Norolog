# Norolog
Compiler for an interesting programming langauge.

What Is Norolog:

The word norolog is a combination of neuro + logic. It was originally meant to be a DSL specifically for neuro-symbolic logical applications (as the name suggests). But i figured out for it to be fast i want it to provide enough low level functionality. So for now, it's a systems programming language.

Specifications:

- Type System: Norolog is statically typed.
- Backend: It binds to C as backend.
- Memory Management: Norolog will not have a GC. it will have a system to handle memory management at compile time. similar to rust, but with none of the rust borrowing rules.
- Other: Generics, functional programming (lambdas and closure), methods, type inference, extension functions, working with references directly (but safely), nullability- mutability-errorability of types.
- GPU Programming: Norolog provides first-class support for general-purpose gpu-programming. currently via Nvidia's CUDA. (it's partly implemented but is in the working!)

Here is what norolog looks like:

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
            val pixelValue = sqrt(w * w)+(h * h);
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
