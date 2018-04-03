package com.rayworks.droidweekly;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.Observable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.rayworks.droidweekly.data.ArticleManager;
import com.rayworks.droidweekly.databinding.LayoutNewsListBinding;
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel;
import com.rayworks.droidweekly.viewmodel.ViewModelFactory;

import java.util.ArrayList;

public class MainActivity
        extends AppCompatActivity /*implements ArticleManager.ArticleDataListener*/ {
    private RecyclerView recyclerView;
    private ArticleAdapter articleAdapter;
    private ProgressBar progressBar;

    private ArticleListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(
                view -> {
                    viewModel.load(true);

                    Snackbar.make(view, "Reloading now...", Snackbar.LENGTH_LONG).show();
                });

        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler_list);
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

        setupViewModel();
    }

    private void setupViewModel() {
        ViewModelFactory factory =
                new ViewModelFactory(getApplication(), ArticleManager.getInstance());
        viewModel = ViewModelProviders.of(this, factory).get(ArticleListViewModel.class);

        LayoutNewsListBinding listBinding = LayoutNewsListBinding.bind(findViewById(R.id.content));
        listBinding.setViewmodel(viewModel);

        viewModel.articleLoaded.addOnPropertyChangedCallback(
                new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable sender, int propertyId) {
                        if (!viewModel.articleLoaded.get()) {

                            // reset for next round
                            viewModel.articleLoaded.set(true);

                            Toast toast =
                                    Toast.makeText(
                                            MainActivity.this,
                                            "Failed to load content, please try again late",
                                            Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        viewModel.load(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
