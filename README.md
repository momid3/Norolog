# Norolog
Compiler for an interesting programming langauge.

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
