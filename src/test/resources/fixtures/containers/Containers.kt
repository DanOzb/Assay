package core.containers


object Registry {
    fun obj_lookup(key: String): Int = 0
}

class WithCompanion(val id: Int) {
    fun member_ofCompanionHost(): Int = id

    companion object {
        fun companion_create(id: Int): WithCompanion = WithCompanion(id)
    }
}

class WithNamedCompanion {
    companion object Factory {
        fun companion_named(): WithNamedCompanion = WithNamedCompanion()
    }
}

class Calculator(val precision: Int, val label: String) {
    fun member_compute(x: Double): Double = x
}

class NoArgs {
    fun member_noArgs(): String = "ok"
}


class CounterCtor(var start: Int) {
    fun member_mutableCtor(): Int = start
}

class CounterProp(val start: Int) {
    var count: Int = start
    fun member_mutableProp(): Int = count
}

class Immutable(val value: Int) {
    fun member_immutable(): Int = value
}

interface Greeter {
    fun iface_greet(name: String): String
    fun iface_default(name: String): String = "hi $name"
}


enum class Color(val hex: String) {
    RED("#f00"),
    GREEN("#0f0"),
    BLUE("#00f");

    fun enum_describe(): String = "$name=$hex"
}

abstract class Shape {
    abstract fun abstract_area(): Double
    fun abstract_describe(): String = "shape"
}

sealed class Result {
    fun sealed_isOk(): Boolean = this is Ok
    class Ok : Result()
}

annotation class Marker

@Marker
class Annotated {
    fun member_annotatedHost(): Int = 1
}

class HiddenConstructor private constructor(val id: Int) {
    fun member_hiddenCtor(): Int = id

    companion object {
        fun companion_ofHidden(id: Int): HiddenConstructor = HiddenConstructor(id)
    }
}

class Outer(val prefix: String) {
    inner class Inner {
        fun inner_member(): String = prefix
    }

    class Nested {
        fun nested_member(): String = "nested"
    }
}

fun local_host(): Int {
    fun local_helper(n: Int): Int = n * 2
    return local_helper(21)
}

fun local_anonymousObjectHost(): Runnable = object : Runnable {
    override fun run() {}
    fun local_insideAnonymous(): Int = 0
}

fun local_classHost(): Int {
    class LocalThing {
        fun local_insideLocalClass(): Int = 7
    }
    return LocalThing().local_insideLocalClass()
}