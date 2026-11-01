package com.example.vagor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

// The in-app Third-Party Licenses list and NOTICE name the same libraries, and the license text the
// app shows is the Apache License 2.0 exactly as Apache publishes it.
class ThirdPartyLicensesTest {
    @Test
    fun theAppListsEveryLibraryInNotice() {
        val listed = inAppText().lines().filter { it.startsWith("• ") }.map { it.removePrefix("• ") }

        assertTrue(listed.isNotEmpty())
        assertEquals(noticeLibraries(), listed)
    }

    @Test
    fun theLicenseTextIsTheUnchangedApacheLicense() {
        val bytes = File("src/main/res/raw/apache_license_2_0.txt").readBytes()
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        // apache.org/licenses/LICENSE-2.0.txt
        assertEquals("cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30", sha256)
    }

    @Test
    fun reflowingJoinsEachParagraphOntoOneLine() {
        assertEquals(
            "Apache License Version 2.0\n\n1. Definitions. \"License\" shall mean",
            reflowParagraphs("   Apache License\n   Version 2.0\n\n   1. Definitions.\n      \"License\" shall mean\n")
        )
    }

    @Test
    fun reflowingTheLicenseKeepsEveryWordInOrder() {
        val license = File("src/main/res/raw/apache_license_2_0.txt").readText()
        val words = { text: String -> text.split(Regex("\\s+")).filter { it.isNotEmpty() } }

        assertEquals(words(license), words(reflowParagraphs(license)))
    }

    // NOTICE's bullets, each joined onto one line, without their ": Apache License 2.0" ending.
    private fun noticeLibraries(): List<String> {
        val bullets = mutableListOf<String>()
        File("../NOTICE").readLines().forEach { line ->
            when {
                line.startsWith("- ") -> bullets += line.removePrefix("- ")
                line.startsWith("  ") && bullets.isNotEmpty() -> bullets[bullets.lastIndex] += " " + line.trim()
            }
        }
        return bullets.map { it.removeSuffix(": Apache License 2.0") }
    }

    // The string resource as Android reads it: the \n escapes become line breaks.
    private fun inAppText(): String {
        val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
            .getElementsByTagName("string")
        val text = (0 until strings.length).map { strings.item(it) }
            .single { it.attributes.getNamedItem("name").nodeValue == "third_party_licenses_text" }
            .textContent
        return text.replace("\\n", "\n").replace("\\'", "'")
    }
}
