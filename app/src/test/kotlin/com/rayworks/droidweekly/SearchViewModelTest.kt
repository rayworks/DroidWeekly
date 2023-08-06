package com.rayworks.droidweekly

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.rayworks.droidweekly.repository.IArticleRepository
import com.rayworks.droidweekly.search.SearchViewModel
import com.rayworks.droidweekly.utils.TestCoroutineRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SearchViewModelTest {
    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private val stateHandle: SavedStateHandle = SavedStateHandle()

    @Mock
    private lateinit var articleRepository: IArticleRepository

    @Test
    fun `after query reset`() {
        testCoroutineRule.runBlockingTest {
            val viewModel = SearchViewModel(articleRepository, Dispatchers.Main)

            viewModel.itemsLiveData.observeForever {
                Assert.assertTrue(it.isEmpty())
                println(">>> obs recved")
            }
            viewModel.setQuery("")

            delay(500)
        }
    }
}
