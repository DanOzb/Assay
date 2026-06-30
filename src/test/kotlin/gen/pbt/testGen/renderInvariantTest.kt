package gen.pbt.testGen

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.example.gen.pbt.models.Bound
import org.example.gen.pbt.models.Rendered
import org.example.gen.pbt.testGen.Fns
import org.example.gen.pbt.testGen.Invs
import org.example.gen.pbt.testGen.renderInvariant

/**
 * Branch coverage for [renderInvariant].
 *
 * Every expected Bound/body string was derived by tracing the real helpers in
 * common.kt (signatureSlots, coreArb/arbForType, SIZED_TYPES, LIST_TYPES).
 * Fixtures live in RenderInvariantFixtures.kt.
 */
class RenderInvariantTest : FunSpec({

    context("involution") {
        test("renders f(f(x)) shouldBe x for a single param") {
            renderInvariant(Fns.negate, Invs.involution) shouldBe Rendered(
                bounds = listOf(Bound("x", "Arb.int()")),
                body = listOf("negate(negate(x)) shouldBe x"),
            )
        }
        test("uses the receiver as the single slot") {
            renderInvariant(Fns.stringReversed, Invs.involution) shouldBe Rendered(
                bounds = listOf(Bound("receiver", "Arb.string()")),
                body = listOf("receiver.reversed().reversed() shouldBe receiver"),
            )
        }
        test("returns null when the param type has no arb") {
            renderInvariant(Fns.negateWidget, Invs.involution) shouldBe null
        }
        test("returns null when there is not exactly one slot") {
            renderInvariant(Fns.now, Invs.involution) shouldBe null
            renderInvariant(Fns.add, Invs.involution) shouldBe null
        }
    }

    context("idempotent") {
        test("renders f(f(x)) shouldBe f(x)") {
            renderInvariant(Fns.abs, Invs.idempotent) shouldBe Rendered(
                bounds = listOf(Bound("x", "Arb.int()")),
                body = listOf("abs(abs(x)) shouldBe abs(x)"),
            )
        }
    }

    context("commutative") {
        test("renders f(a, b) shouldBe f(b, a)") {
            renderInvariant(Fns.add, Invs.commutative) shouldBe Rendered(
                bounds = listOf(Bound("a", "Arb.int()"), Bound("b", "Arb.int()")),
                body = listOf("add(a, b) shouldBe add(b, a)"),
            )
        }
        test("renders the receiver form when fn has a receiver") {
            renderInvariant(Fns.intPlus, Invs.commutative) shouldBe Rendered(
                bounds = listOf(Bound("receiver", "Arb.int()"), Bound("other", "Arb.int()")),
                body = listOf("receiver.plus(other) shouldBe other.plus(receiver)"),
            )
        }
        test("returns null when operands have different types") {
            renderInvariant(Fns.repeatStr, Invs.commutative) shouldBe null
        }
        test("returns null when the operand type has no arb") {
            renderInvariant(Fns.mergeWidget, Invs.commutative) shouldBe null
        }
        test("returns null when arity is not two") {
            renderInvariant(Fns.negate, Invs.commutative) shouldBe null
        }
    }

    context("associative") {
        test("renders f(f(a, b), c) shouldBe f(a, f(b, c)) with a fresh third operand") {
            renderInvariant(Fns.add, Invs.associative) shouldBe Rendered(
                bounds = listOf(
                    Bound("a", "Arb.int()"),
                    Bound("b", "Arb.int()"),
                    Bound("c", "Arb.int()"),
                ),
                body = listOf("add(add(a, b), c) shouldBe add(a, add(b, c))"),
            )
        }
        test("avoids a name collision when a param is already named c") {
            renderInvariant(Fns.combineCd, Invs.associative) shouldBe Rendered(
                bounds = listOf(
                    Bound("c", "Arb.int()"),
                    Bound("d", "Arb.int()"),
                    Bound("c2", "Arb.int()"),
                ),
                body = listOf("combine(combine(c, d), c2) shouldBe combine(c, combine(d, c2))"),
            )
        }
        test("returns null when operands are not a binary same-type pair") {
            renderInvariant(Fns.repeatStr, Invs.associative) shouldBe null
        }
        test("associative: receiver form") {
            renderInvariant(Fns.intPlus, Invs.associative) shouldBe Rendered(
                bounds = listOf(Bound("receiver", "Arb.int()"), Bound("other", "Arb.int()"), Bound("c", "Arb.int()")),
                body = listOf("receiver.plus(other).plus(c) shouldBe receiver.plus(other.plus(c))"),
            )
        }
    }

    context("identity_element") {
        test("returns null when value is missing") {
            renderInvariant(Fns.add, Invs.identityNoValue) shouldBe null
        }
        test("returns null when fn is not binary") {
            renderInvariant(Fns.negate, Invs.identityZero) shouldBe null
        }
        test("identity: returns null when the operand type has no arb") {
            renderInvariant(Fns.mergeWidget, Invs.identityZero) shouldBe null   // reuses mergeWidget
        }
    }

    context("length_preserving") {
        test("compares output size to the input slot size") {
            renderInvariant(Fns.reverseList, Invs.lengthPreserving) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int())")),
                body = listOf("reverse(xs).size shouldBe xs.size"),
            )
        }
        test("uses .length for String slots") {
            renderInvariant(Fns.reverseString, Invs.lengthPreserving) shouldBe Rendered(
                bounds = listOf(Bound("s", "Arb.string()")),
                body = listOf("reverseString(s).length shouldBe s.length"),
            )
        }
        test("uses the args hint to disambiguate multiple sized slots") {
            renderInvariant(Fns.zip, Invs.lengthPreservingHinted) shouldBe Rendered(
                bounds = listOf(
                    Bound("a", "Arb.list(Arb.int())"),
                    Bound("b", "Arb.list(Arb.int())"),
                ),
                body = listOf("zip(a, b).size shouldBe a.size"),
            )
        }
        test("returns null when the sized slot is ambiguous and unhinted") {
            renderInvariant(Fns.zip, Invs.lengthPreserving) shouldBe null
        }
        test("returns null when the return type is not sized") {
            renderInvariant(Fns.count, Invs.lengthPreserving) shouldBe null
        }
        test("length: hint selects the second slot, not the first") {
            renderInvariant(Fns.zip, Invs.lengthPreservingHintB) shouldBe Rendered(
                bounds = listOf(Bound("a", "Arb.list(Arb.int())"), Bound("b", "Arb.list(Arb.int())")),
                body = listOf("zip(a, b).size shouldBe b.size"),
            )
        }
        test("length: an invalid hint falls back to the single candidate") {
            renderInvariant(Fns.reverseList, Invs.lengthPreservingBadHint) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int())")),
                body = listOf("reverse(xs).size shouldBe xs.size"),
            )
        }
        test("length: an invalid hint with ambiguous slots returns null") {
            renderInvariant(Fns.zip, Invs.lengthPreservingBadHint) shouldBe null
        }
        test("length: returns null when no input is sized") {
            renderInvariant(Fns.fill, Invs.lengthPreserving) shouldBe null
        }
        test("length: receiver as the sized slot") {
            renderInvariant(Fns.dropFirst, Invs.lengthPreserving) shouldBe Rendered(
                bounds = listOf(Bound("receiver", "Arb.list(Arb.int())")),
                body = listOf("receiver.dropFirst().size shouldBe receiver.size"),
            )
        }
        test("uses ?. on a nullable return type") {
            renderInvariant(Fns.reverseListNullableOut, Invs.lengthPreserving) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int())")),
                body = listOf("reverse(xs)?.size shouldBe xs.size"),
            )
        }
        test("uses ?. on a nullable input slot and generates .orNull()") {
            renderInvariant(Fns.reverseListNullableIn, Invs.lengthPreserving) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int()).orNull()")),
                body = listOf("reverse(xs).size shouldBe xs?.size"),
            )
        }
    }

    context("permutation_invariant") {
        test("shuffles the single list slot") {
            renderInvariant(Fns.sortList, Invs.permutationInvariant) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int())")),
                body = listOf("sort(xs) shouldBe sort(xs.shuffled())"),
            )
        }
        test("shuffles only the hinted slot") {
            renderInvariant(Fns.zip, Invs.permutationInvariantHinted) shouldBe Rendered(
                bounds = listOf(
                    Bound("a", "Arb.list(Arb.int())"),
                    Bound("b", "Arb.list(Arb.int())"),
                ),
                body = listOf("zip(a, b) shouldBe zip(a.shuffled(), b)"),
            )
        }
        test("returns null when the list slot is ambiguous and unhinted") {
            renderInvariant(Fns.zip, Invs.permutationInvariant) shouldBe null
        }
        test("permutation: hint shuffles the second slot, not the first") {
            renderInvariant(Fns.zip, Invs.permutationInvariantHintB) shouldBe Rendered(
                bounds = listOf(Bound("a", "Arb.list(Arb.int())"), Bound("b", "Arb.list(Arb.int())")),
                body = listOf("zip(a, b) shouldBe zip(a, b.shuffled())"),
            )
        }
        test("permutation: returns null when no input is a list") {
            renderInvariant(Fns.abs, Invs.permutationInvariant) shouldBe null   // reuses abs
        }
        test("permutation: shuffles the receiver") {
            renderInvariant(Fns.dropFirst, Invs.permutationInvariant) shouldBe Rendered(
                bounds = listOf(Bound("receiver", "Arb.list(Arb.int())")),
                body = listOf("receiver.dropFirst() shouldBe receiver.shuffled().dropFirst()"),
            )
        }
    }

    context("never_throws") {
        test("wraps the call in shouldNotThrowAny") {
            renderInvariant(Fns.parsePositive, Invs.neverThrows) shouldBe Rendered(
                bounds = listOf(Bound("s", "Arb.string()")),
                body = listOf("shouldNotThrowAny { parsePositive(s) }"),
            )
        }
        test("never_throws: zero-arg function yields empty bounds") {
            renderInvariant(Fns.now, Invs.neverThrows) shouldBe Rendered(
                bounds = emptyList(),
                body = listOf("shouldNotThrowAny { now() }"),
            )
        }
    }

    context("output_constraint") {
        test("binds result and asserts the predicate") {
            renderInvariant(Fns.parsePositive, Invs.outputConstraint) shouldBe Rendered(
                bounds = listOf(Bound("s", "Arb.string()")),
                body = listOf("val result = parsePositive(s)", "result >= 0 shouldBe true"),
            )
        }
        test("returns null when predicate is missing") {
            renderInvariant(Fns.parsePositive, Invs.outputConstraintNoPredicate) shouldBe null
        }
        test("returns null when a parameter is named result") {
            renderInvariant(Fns.scoreResult, Invs.outputConstraint) shouldBe null
        }
    }

    context("oracle") {
        test("compares result against the reference expression") {
            renderInvariant(Fns.sortList, Invs.oracle) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int())")),
                body = listOf("val result = sort(xs)", "result shouldBe xs.sorted()"),
            )
        }
        test("returns null when reference is missing") {
            renderInvariant(Fns.sortList, Invs.oracleNoReference) shouldBe null
        }
        test("returns null when a parameter is named result") {
            renderInvariant(Fns.scoreResult, Invs.oracle) shouldBe null
        }
    }

    context("custom") {
        test("binds result and appends the custom code") {
            renderInvariant(Fns.sortList, Invs.custom) shouldBe Rendered(
                bounds = listOf(Bound("xs", "Arb.list(Arb.int())")),
                body = listOf("val result = sort(xs)", "result shouldHaveSize xs.size"),
            )
        }
        test("returns null when code is missing") {
            renderInvariant(Fns.sortList, Invs.customNoCode) shouldBe null
        }
    }

    context("unknown kind") {
        test("returns null") {
            renderInvariant(Fns.negate, Invs.unknownKind) shouldBe null
        }
    }

    test("returns null when a parameter is named result") {
        renderInvariant(Fns.scoreResult, Invs.custom) shouldBe null
    }
})