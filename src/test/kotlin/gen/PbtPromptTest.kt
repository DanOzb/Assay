package gen

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldNotBeEmpty
import org.example.gen.pbt.PbtPrompt

/**
 *
 * Test incase I change schema location or name
 *
 */

class PbtPromptTest: FunSpec({

    test("the pbt schema loads"){
        val schema = PbtPrompt().getSchema()
        schema.shouldNotBeEmpty()
    }
})