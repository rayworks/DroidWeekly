package com.rayworks.droidweekly

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rayworks.droidweekly.databinding.ActivitySearchBinding
import com.rayworks.droidweekly.extension.scoped
import com.rayworks.droidweekly.model.ArticleItem
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.repository.ArticleManager
import com.rayworks.droidweekly.repository.ArticleManager.ArticleDataListener
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit

/***
 * The page shows the search result based on cached historical articles
 */
class SearchActivity : AppCompatActivity(), ArticleDataListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var binding : ActivitySearchBinding
    private var searchView: SearchView? = null
    private var disposable: Disposable? = null


    /***
     *  A presenter handles the searching by keywords.
     */
    class SearchPresenter {
        /***
         * Search specified keywords
         */
        fun search(s: String, articleListener: WeakReference<ArticleDataListener>) {
            ArticleManager.getInstance()
                    .search(s, articleListener)
        }

        /***
         * Dispose the unused resources
         */
        fun dispose() {
            ArticleManager.getInstance().dispose()
        }
    }

    private val presenter by scoped { SearchPresenter() }

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
        disposable = RxSearchObservable.fromView(searchView)
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
                    presenter.search(s, articleListener)
                    Observable.just(s)
                }
                .subscribe { s -> println("Searching for key : $s") }
    }

    private fun resetResult() {
        runOnUiThread {
            if (!this@SearchActivity.isFinishing && recyclerView != null) {
                articleAdapter!!.update(Collections.emptyList())
            }
        }
    }

    private var articleListener: WeakReference<ArticleDataListener> = WeakReference(this@SearchActivity)

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

    override fun onLoadError(err: String) {
        Toast.makeText(this, "Error: $err", Toast.LENGTH_SHORT).show()
    }

    override fun onOldRefItemsLoaded(itemRefs: List<OldItemRef>) {
        /** None logic *  */
    }

    override fun onComplete(items: List<ArticleItem>) {
        articleAdapter!!.update(items)
    }

    override fun onDestroy() {
        if (disposable != null) {
            presenter.dispose()

            disposable!!.dispose()
        }
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun launch(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }
}