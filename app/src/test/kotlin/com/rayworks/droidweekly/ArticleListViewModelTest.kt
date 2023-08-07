package com.rayworks.droidweekly

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.IArticleRepository
import com.rayworks.droidweekly.utils.TestCoroutineRule
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.junit.MockitoJUnitRunner

/***
 * The mockito test for [ArticleListViewModel]
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ArticleListViewModelTest {
    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private val stateHandle: SavedStateHandle = SavedStateHandle()

    @Mock
    private lateinit var articleRepository: IArticleRepository

    @Mock
    private lateinit var observable: Observer<List<ArticleItem>>

    /***
     * Test method for loading article content
     */
    @Test
    fun `test basic loading data`() {
        testCoroutineRule.runBlockingTest {
            doReturn(null).`when`(articleRepository).loadData()
            doReturn(MutableStateFlow<List<OldItemRef>>(listOf())).`when`(articleRepository).refList
            doReturn(MutableStateFlow<List<OldItemRef>>(listOf())).`when`(articleRepository).articleList

            val viewModel = ArticleListViewModel(stateHandle, articleRepository)
            viewModel.load(true)

            Mockito.verify(articleRepository).loadData()
        }
    }
}
