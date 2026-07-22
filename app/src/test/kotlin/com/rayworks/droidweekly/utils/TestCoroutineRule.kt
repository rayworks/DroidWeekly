package com.rayworks.droidweekly.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/***
 * The custom Coroutine Test rule.
 */
@ExperimentalCoroutinesApi
class TestCoroutineRule : TestRule {

    private val testCoroutineDispatcher = UnconfinedTestDispatcher()

    private val testCoroutineScope = TestScope(testCoroutineDispatcher)

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            Dispatchers.setMain(testCoroutineDispatcher)

            base.evaluate()

            Dispatchers.resetMain()
        }
    }

    /***
     * Convenience method for calling [runTest] on an existing [TestScope] with Coroutine.
     */
    fun runTest(block: suspend TestScope.() -> Unit) =
        testCoroutineScope.runTest { block() }
}
