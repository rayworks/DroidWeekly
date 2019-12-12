package com.rayworks.droidweekly;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.rayworks.droidweekly.model.ArticleItem;
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.repository.ArticleManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class SearchActivity extends AppCompatActivity
        implements ArticleManager.ArticleDataListener {

    private RecyclerView recyclerView;
    private ArticleAdapter articleAdapter;
    private SearchView searchView;
    private Disposable disposable;

    public static void launch(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);

        recyclerView = findViewById(R.id.result_list);
        articleAdapter = new ArticleAdapter(this, new ArrayList<>());
        articleAdapter.setViewArticleListener(
                url -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                });

        recyclerView.setAdapter(articleAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, layoutManager.getOrientation()));
    }

    private void setupSearchAction() {
        disposable =
                RxSearchObservable.fromView(searchView)
                        .debounce(300, TimeUnit.MILLISECONDS)
                        .filter(
                                s -> {
                                    boolean isEmpty = s.isEmpty();
                                    if (isEmpty) {
                                        resetResult();
                                    }

                                    return !isEmpty;
                                })
                        .distinctUntilChanged()
                        .switchMap(
                                new Function<String, ObservableSource<? extends String>>() {
                                    @Override
                                    public ObservableSource<? extends String> apply(String s)
                                            throws Exception {

                                        // already an async operation
                                        ArticleManager.getInstance()
                                                .search(s, getArticleListener());

                                        return Observable.just(s);
                                    }
                                })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        System.out.println("Searching for key : " + s);
                                    }
                                });
    }

    private void resetResult() {
        runOnUiThread(
                () -> {
                    if (!SearchActivity.this.isFinishing() && recyclerView != null) {
                        articleAdapter.update(Collections.EMPTY_LIST);
                    }
                });
    }

    @NonNull
    private WeakReference<ArticleManager.ArticleDataListener> getArticleListener() {
        return new WeakReference<>(SearchActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        final MenuItem item = menu.findItem(R.id.action_search);

        searchView = (SearchView) item.getActionView();
        setupSearchAction();

        // expands the SearchView automatically
        searchView.setIconified(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoadError(String err) {
        Toast.makeText(this, "Error: " + err, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onOldRefItemsLoaded(List<OldItemRef> itemRefs) {
        /** None logic * */
    }

    @Override
    public void onComplete(List<ArticleItem> items) {
        articleAdapter.update(items);
    }

    @Override
    protected void onDestroy() {
        if (disposable != null) {
            disposable.dispose();
        }

        super.onDestroy();
    }
}
