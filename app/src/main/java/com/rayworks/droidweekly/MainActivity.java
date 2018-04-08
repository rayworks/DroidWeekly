package com.rayworks.droidweekly;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.databinding.Observable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
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
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel;
import com.rayworks.droidweekly.viewmodel.ViewModelFactory;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String MENU_ITEM_ID = "menu_item_id";
    private final int MENU_ID_BASE = Menu.FIRST + 0xFF;
    private RecyclerView recyclerView;
    private ArticleAdapter articleAdapter;
    private ProgressBar progressBar;
    private ArticleListViewModel viewModel;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<OldItemRef> oldItemRefList;

    private int selectedItemId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(MENU_ITEM_ID, -1);
            System.out.println(">>> Item position restored : " + selectedItemId);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_24dp);

        navigationView = findViewById(R.id.nav_view);
        drawerLayout = findViewById(R.id.drawer_layout);

        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    int id = menuItem.getItemId();
                    selectedItemId = id;

                    if (oldItemRefList != null) {
                        OldItemRef itemRef = oldItemRefList.get(id - MENU_ID_BASE);

                        viewModel.loadBy(itemRef.getRelativePath());

                        menuItem.setChecked(true);
                        drawerLayout.closeDrawers();
                    }
                    return true;
                });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(
                view -> {
                    viewModel.load(true);

                    // reset menu item selection
                    setMenuItemCheckStatus(navigationView.getMenu(), false);
                    selectedItemId = -1;

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (selectedItemId > 0) {
            outState.putInt(MENU_ITEM_ID, selectedItemId);
        }

        super.onSaveInstanceState(outState);
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

        viewModel.itemRefs.addOnPropertyChangedCallback(
                new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable sender, int propertyId) {
                        oldItemRefList = viewModel.itemRefs.get();
                        if (oldItemRefList == null || oldItemRefList.isEmpty()) {
                            return;
                        }

                        Menu menu = navigationView.getMenu();
                        menu.clear();

                        int base = MENU_ID_BASE;
                        for (OldItemRef itemRef : oldItemRefList) {
                            int pos = base++;
                            menu.add(0, pos, pos, itemRef.getTitle());
                        }

                        // set exclusive item check
                        menu.setGroupCheckable(0, true, true);

                        setMenuItemCheckStatus(menu, true);

                        navigationView.invalidate();
                    }
                });
    }

    private void setMenuItemCheckStatus(Menu menu, boolean checked) {
        if (selectedItemId > 0) {
            menu.getItem(selectedItemId - MENU_ID_BASE).setChecked(checked);
        }
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
        switch (id) {
            case R.id.action_settings:
                break;
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
