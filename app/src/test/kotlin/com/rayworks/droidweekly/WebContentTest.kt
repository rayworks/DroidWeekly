package com.rayworks.droidweekly

import com.rayworks.droidweekly.repository.WebContentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebContentTest {
    private val parser = WebContentParser()

    @Test
    fun `latest issue id parsing`() {
        val content = ClassLoader.getSystemResource("weekly666.html").readText()
        val pair = parser.parse(content)

        val refs = pair.second
        assertTrue(refs.isNotEmpty())
        assertEquals(666, refs[0].issueId)
    }

    @Test
    fun `Latest issue content parsing`() {
        val content = ClassLoader.getSystemResource("weekly.html").readText()
        val pair = parser.parse(content)

        val refs = pair.second
        assertTrue(refs.isNotEmpty())
        assertEquals(399, refs[0].issueId)

        assertTrue(pair.first.isNotEmpty())

        val prefix = "/issues/issue-"
        assertTrue("First issue should have valid issue path", refs.first().relativePath.startsWith(prefix))
        assertTrue("Last visible issue should have valid issue path", refs.last().relativePath.startsWith(prefix))
    }

    @Test
    fun `Historical issue content parsing`() {
        val content = ClassLoader.getSystemResource("weekly_old.html").readText()
        val pair = parser.parse(content)

        val refs = pair.second
        assertTrue(refs.isEmpty())

        assertTrue(pair.first.isNotEmpty())
        assertEquals("Articles & Tutorials", pair.first[0].title)
    }

    @Test
    fun `New web content parsing`() {
        val content = ClassLoader.getSystemResource("weekly501.html").readText()
        val pair = parser.parse(content)

        val refs = pair.second
        assertTrue(refs.isNotEmpty())
        assertEquals(501, refs[0].issueId)

        assertTrue(pair.first.isNotEmpty())

        val prefix = "/issues/issue-"
        assertTrue("First issue should have valid issue path", refs.first().relativePath.startsWith(prefix))
        assertTrue("Last visible issue should have valid issue path", refs.last().relativePath.startsWith(prefix))
    }
}
