package core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.example.core.Callability
import org.example.core.ContainerKind
import org.example.core.InstanceStructure
import org.example.core.ParsedFunction
import org.example.core.Parser
import java.nio.file.Paths

class ParseContainersTest : FunSpec({

    val parser = Parser()

    lateinit var sharedResults: List<ParsedFunction>

    beforeSpec {
        val resourceUri = this::class.java.getResource("/fixtures/containers")?.toURI()
            ?: error("Fixture directory not found.")
        sharedResults = parser.inspectDir(Paths.get(resourceUri))
    }

    fun findFunction(name: String): ParsedFunction =
        sharedResults.find { it.name == name }
            ?: error("Function '$name' was not found")


    test("function in object is SINGLETON with Singleton structure") {
        val fn = findFunction("obj_lookup")
        fn.callability shouldBe Callability.SINGLETON
        val container = fn.container.shouldNotBeNull()
        container.name shouldBe "Registry"
        container.kind shouldBe ContainerKind.OBJECT
        container.instanceStructure shouldBe InstanceStructure.Singleton
    }

    test("function in companion object is SINGLETON with COMPANION_OBJECT kind") {
        val fn = findFunction("companion_create")
        fn.callability shouldBe Callability.SINGLETON
        val container = fn.container.shouldNotBeNull()
        container.kind shouldBe ContainerKind.COMPANION_OBJECT
        container.instanceStructure shouldBe InstanceStructure.Singleton
    }

    test("named companion object keeps its declared name") {
        val container = findFunction("companion_named").container.shouldNotBeNull()
        container.kind shouldBe ContainerKind.COMPANION_OBJECT
        container.name shouldBe "Factory"
    }

    test("member function requires an instance and exposes constructor params") {
        val fn = findFunction("member_compute")
        fn.callability shouldBe Callability.REQUIRES_INSTANCE
        val container = fn.container.shouldNotBeNull()
        container.kind shouldBe ContainerKind.CLASS
        container.fqName shouldBe "core.containers.Calculator"

        val structure = container.instanceStructure
            .shouldBeInstanceOf<InstanceStructure.Construct>()
        structure.params.map { it.name } shouldContainExactly listOf("precision", "label")
        structure.params.map { it.type } shouldContainExactly listOf("Int", "String")
    }

    test("class without primary constructor params yields empty Construct") {
        val container = findFunction("member_noArgs").container.shouldNotBeNull()
        val structure = container.instanceStructure
            .shouldBeInstanceOf<InstanceStructure.Construct>()
        structure.params shouldBe emptyList()
    }

    test("top-level function has no container") {
        val fn = findFunction("local_host")
        fn.callability shouldBe Callability.TOP_LEVEL
        fn.container.shouldBeNull()
    }

    test("var in primary constructor marks container as mutable") {
        findFunction("member_mutableCtor").container.shouldNotBeNull()
            .hasMutableState.shouldBeTrue()
    }

    test("var property in body marks container as mutable") {
        findFunction("member_mutableProp").container.shouldNotBeNull()
            .hasMutableState.shouldBeTrue()
    }

    test("val-only class is not marked mutable") {
        findFunction("member_immutable").container.shouldNotBeNull()
            .hasMutableState.shouldBeFalse()
    }

    test("interface function is REQUIRES_INSTANCE and NotConstructible") {
        val fn = findFunction("iface_greet")
        fn.callability shouldBe Callability.REQUIRES_INSTANCE
        val container = fn.container.shouldNotBeNull()
        container.kind shouldBe ContainerKind.INTERFACE
        container.instanceStructure shouldBe InstanceStructure.NotConstructible("interface")
    }

    test("interface default method parses body") {
        val fn = findFunction("iface_default")
        fn.body.shouldNotBeNull()
        fn.container.shouldNotBeNull().kind shouldBe ContainerKind.INTERFACE
    }

    test("enum member function exposes enum entries") {
        val fn = findFunction("enum_describe")
        fn.callability shouldBe Callability.REQUIRES_INSTANCE
        val container = fn.container.shouldNotBeNull()
        container.kind shouldBe ContainerKind.ENUM
        container.instanceStructure shouldBe
                InstanceStructure.EnumEntries(listOf("RED", "GREEN", "BLUE"))
    }

    test("abstract class is NotConstructible(abstract class)") {
        findFunction("abstract_describe").container.shouldNotBeNull()
            .instanceStructure shouldBe InstanceStructure.NotConstructible("abstract class")
    }

    test("sealed class is NotConstructible(sealed class)") {
        findFunction("sealed_isOk").container.shouldNotBeNull()
            .instanceStructure shouldBe InstanceStructure.NotConstructible("sealed class")
    }

    test("private primary constructor is NotConstructible") {
        findFunction("member_hiddenCtor").container.shouldNotBeNull()
            .instanceStructure shouldBe
                InstanceStructure.NotConstructible("non-public primary constructor")
    }

    test("annotated class is still constructible") {
        findFunction("member_annotatedHost").container.shouldNotBeNull()
            .instanceStructure.shouldBeInstanceOf<InstanceStructure.Construct>()
    }

    test("inner class is flagged isInner") {
        val container = findFunction("inner_member").container.shouldNotBeNull()
        container.name shouldBe "Inner"
        container.isInner.shouldBeTrue()
    }

    test("nested (non-inner) class is not flagged isInner") {
        findFunction("nested_member").container.shouldNotBeNull()
            .isInner.shouldBeFalse()
    }

    test("local function is LOCAL") {
        findFunction("local_helper").callability shouldBe Callability.LOCAL
    }

    test("function inside anonymous object is LOCAL") {
        findFunction("local_insideAnonymous").callability shouldBe Callability.LOCAL
    }

    test("function inside local class is LOCAL") {
        findFunction("local_insideLocalClass").callability shouldBe Callability.LOCAL
    }
})