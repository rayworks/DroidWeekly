package com.rayworks.droidweekly

import com.rayworks.droidweekly.repository.WebContentParser
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object HtmlParsingSpec : Spek({
    val parser by memoized { WebContentParser() }

    describe("Latest issue content parsing") {

        it("The latest Html content should be parsed successfully") {
            val content = ClassLoader.getSystemResource("weekly.html").readText()
            val pair = parser.parse(content)

            val refs = pair.second
            assertTrue(refs.isNotEmpty())
            assertEquals(399, refs[0].issueId)

            assertTrue(pair.first.isNotEmpty())
        }
    }

    describe("Historical issue content parsing") {

        it("Historical issue content should be parsed successfully") {
            val content = ClassLoader.getSystemResource("weekly_old.html").readText()
            val pair = parser.parse(content)

            val refs = pair.second
            assertTrue(refs.isEmpty())

            assertTrue(pair.first.isNotEmpty())
            assertEquals("Articles & Tutorials", pair.first[0].title)
        }

    }
})