package io.withtwoemms.github.example

fun main(args: Array<String>) {
    val example = Example()
    println(example.greeter.say("Hello!"))
}

class Example(val greeter: Greeter = Greeter())
