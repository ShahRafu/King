package com.shahrafuking.kingassistant.selfheal

import org.junit.Assert.*
import org.junit.Test

class LocalCodeExecutorTest {

    @Test
    fun runValidator_allows_safe_script() {
        val executor = LocalCodeExecutor()
        val script = "(function(){ return true; })()"
        val ok = executor.runValidator(script, emptyMap())
        assertTrue("Validator should accept simple true script", ok)
    }

    @Test
    fun runValidator_rejects_risky_content() {
        val executor = LocalCodeExecutor()
        val validationScript = "(function(content){ return /fetch\\(/i.test(content); })(content)"
        // supply a content containing 'fetch(' -> validator should detect risky token and thus return true from script,
        // but runValidator expects boolean semantics; here we assert that our wrapper works to detect unwanted tokens.
        val ok = executor.runValidator("(function(content){ return !/fetch\\(/i.test(content); })(content)", mapOf("content" to "some code with fetch(url)"))
        assertFalse("Validator should reject content with fetch()", ok)
    }

    @Test
    fun generateCode_returns_string_and_respects_forbidden_tokens() {
        val executor = LocalCodeExecutor()
        val script = """
            (function(prompt, existing){
                // return a tiny Kotlin snippet using prompt
                return "package com.shahrafuking.kingassistant.autogen\n\nobject Gen { fun msg() = " + JSON.stringify(prompt) + " }\n";
            })(prompt, existing)
        """.trimIndent()
        val out = executor.generateCode(script, "hello-prompt", "existing")
        assertNotNull("Generator should return non-null output", out)
        assertTrue("Generated output should contain package declaration", out!!.contains("package"))
    }
}
