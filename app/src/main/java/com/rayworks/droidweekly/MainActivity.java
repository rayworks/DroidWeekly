package com.rayworks.droidweekly;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.databinding.Observable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.rayworks.droidweekly.databinding.ActivityMainBinding;
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.repository.ArticleRepository;
import com.rayworks.droidweekly.repository.WebContentParser;
import com.rayworks.droidweekly.repository.database.ArticleDao;
import com.rayworks.droidweekly.repository.database.IssueDatabase;
import com.rayworks.droidweekly.repository.database.IssueDatabaseKt;
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel;
import com.rayworks.droidweekly.viewmodel.ViewModelFactory;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String MENU_ITEM_ID = "menu_item_id";
    private final int MENU_ID_BASE = Menu.FIRST + 0xFF;
    private ArticleListViewModel viewModel;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<OldItemRef> oldItemRefList;

    private int selectedItemId = MENU_ID_BASE;

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
                    /*viewModel.load(true);

                    // reset menu item selection
                    setMenuItemCheckStatus(navigationView.getMenu(), false);
                    selectedItemId = -1;

                    Snackbar.make(view, "Reloading now...", Snackbar.LENGTH_LONG).show();*/

                    SearchActivity.launch(MainActivity.this);
                });

        RecyclerView recyclerView = findViewById(R.id.recycler_list);
        ArticleAdapter articleAdapter = new ArticleAdapter(this, new ArrayList<>());
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
        IssueDatabase database = IssueDatabaseKt.getDatabase(this);
        ArticleDao articleDao = database.articleDao();

        Application context = getApplication();
        SharedPreferences preferences =
                context.getSharedPreferences(
                        ArticleRepository.Companion.getISSUE_INFO(), Context.MODE_PRIVATE);

        ViewModelFactory factory =
                new ViewModelFactory(context, new ArticleRepository(articleDao, preferences, new WebContentParser()));
        viewModel = new ViewModelProvider(this, factory).get(ArticleListViewModel.class) ;

        ActivityMainBinding dataBinding = ActivityMainBinding.bind(findViewById(R.id.drawer_layout));
        dataBinding.setViewmodel(viewModel);

        // enables MutableLiveData to be update on your UI
        dataBinding.setLifecycleOwner(this);

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

        viewModel
                .getItemRefs()
                .observe(
                        this,
                        oldItemRefs -> {
                            oldItemRefList = oldItemRefs;
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

                            // default selection
                            setMenuItemCheckStatus(menu, true);

                            navigationView.invalidate();
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
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
