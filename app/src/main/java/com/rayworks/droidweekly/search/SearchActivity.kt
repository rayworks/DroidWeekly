package com.rayworks.droidweekly.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rayworks.droidweekly.R
import com.rayworks.droidweekly.databinding.ActivitySearchBinding
import com.rayworks.droidweekly.ui.component.ArticleAdapter
import com.rayworks.droidweekly.utils.RxSearchObservable
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/***
 * The page shows the search result based on cached historical articles
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity()/*, ArticleDataListener*/ {
    private lateinit var recyclerView: RecyclerView
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var binding: ActivitySearchBinding
    private var searchView: SearchView? = null

    val viewModel: ArticleListViewModel by viewModels()

    private val scopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)

        recyclerView = binding.resultList
        articleAdapter = ArticleAdapter(this, ArrayList())
        articleAdapter.setViewArticleListener { url: String? ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }

        recyclerView.apply {
            adapter = articleAdapter

            val layoutManager = LinearLayoutManager(this@SearchActivity)
            this.layoutManager = layoutManager

            addItemDecoration(
                    DividerItemDecoration(this@SearchActivity, layoutManager.orientation))
        }
    }

    private fun setupSearchAction() {
        RxSearchObservable.fromView(searchView)
            .debounce(300, TimeUnit.MILLISECONDS)
            .filter { s: String ->
                val isEmpty = s.isEmpty()
                if (isEmpty) {
                    resetResult()
                }
                !isEmpty
            }
            .distinctUntilChanged()
            .switchMap { s -> // already an async operation
                lifecycleScope.launch() {
                    val searchArticles = viewModel.searchArticles(s)
                    articleAdapter.update(searchArticles)
                }

                Observable.just(s)
            }.doOnDispose {
                Timber.w(">>> Disposing the search observable")
            }
            .autoDispose(scopeProvider)
            .subscribe { s -> println("Searching for key : $s") }
    }

    private fun resetResult() {
        runOnUiThread {
            if (!this@SearchActivity.isFinishing) {
                articleAdapter.update(Collections.emptyList())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        val item = menu.findItem(R.id.action_search)
        searchView = item.actionView as SearchView
        setupSearchAction()

        // expands the SearchView automatically
        searchView!!.isIconified = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        fun launch(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }
}
