package com.rayworks.droidweekly.dashboard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.Observable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.rayworks.droidweekly.App;
import com.rayworks.droidweekly.R;
import com.rayworks.droidweekly.databinding.ActivityMainBinding;
import com.rayworks.droidweekly.di.KeyValueStorage;
import com.rayworks.droidweekly.model.OldItemRef;
import com.rayworks.droidweekly.model.ThemeOption;
import com.rayworks.droidweekly.repository.ArticleRepository;
import com.rayworks.droidweekly.search.SearchActivity;
import com.rayworks.droidweekly.ui.component.ArticleAdapter;
import com.rayworks.droidweekly.utils.ImageUtilsKt;
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel;
import com.rayworks.droidweekly.viewmodel.ViewModelFactory;
import com.yalantis.ucrop.UCrop;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import static com.rayworks.droidweekly.repository.Constants.LATEST_ISSUE_ID;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 0xF1;
    private final int REQUEST_CAMERA_ACCESS_PERMISSION = 0xF2;

    private final int REQUEST_CODE_PHONE_ALBUM = 0xE1;
    private final int REQUEST_CODE_PHONE_CAMERA = 0xE2;

    private final String capturedImageName = "aw_captured_img.png";

    private final int MENU_ID_BASE = Menu.FIRST + 0xFF;
    private ArticleListViewModel viewModel;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<OldItemRef> oldItemRefList;

    private int selectedItemId = MENU_ID_BASE;
    private View headerView;
    private ImageView avatarImageView;

    @Inject
    ArticleRepository articleRepository;

    @Inject
    KeyValueStorage keyValueStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_24dp);

        setupNavigationDrawer();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> SearchActivity.launch(MainActivity.this));

        setupArticleList();

        setupViewModel();

        int lastSelected = viewModel.getSelectedItemId();
        if (lastSelected > 0) {
            selectedItemId = lastSelected;
        }
    }

    private void setupArticleList() {
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
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        headerView = LayoutInflater.from(this).inflate(R.layout.view_layout_nav_header, navigationView, false);
        avatarImageView = headerView.findViewById(R.id.avatar);
        avatarImageView.setOnClickListener(v -> promptPickingImage());
        String localAvatar = App.get().getLocalAvatar();
        if (!TextUtils.isEmpty(localAvatar)) {
            loadAvatarIfAny(Uri.parse(localAvatar), avatarImageView);
        }
        navigationView.addHeaderView(headerView);

        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    int id = menuItem.getItemId();
                    selectedItemId = id;

                    viewModel.setSelectedItemId(id);

                    if (oldItemRefList != null) {
                        OldItemRef itemRef = oldItemRefList.get(id - MENU_ID_BASE);

                        viewModel.loadBy(itemRef.getRelativePath());

                        menuItem.setChecked(true);
                        drawerLayout.closeDrawers();
                    }
                    return true;
                });
    }

    private void promptPickingImage() {
        PopupMenu popupMenu = new PopupMenu(this, headerView, Gravity.CENTER_HORIZONTAL);
        MenuInflater inflater = popupMenu.getMenuInflater();
        Menu menu = popupMenu.getMenu();
        inflater.inflate(R.menu.menu_image_selection, menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.from_album) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
                } else {
                    requestPickImage();
                }
            } else {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_ACCESS_PERMISSION);
                } else {
                    requestTakeShot();
                }
            }
            return true;
        });
        popupMenu.show();
    }

    private void requestTakeShot() {
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                ImageUtilsKt.getCapturedImageOutputUri(this, capturedImageName));
        startActivityForResult(intent, REQUEST_CODE_PHONE_CAMERA);
    }

    private void requestPickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PHONE_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            Timber.w(">>> resultCode : $resultCode for original request $requestCode with data : $data");
            return;
        }

        switch (requestCode) {
            case UCrop.RESULT_ERROR: {
                if (data != null) {
                    Throwable error = UCrop.getError(data);
                    if (error != null)
                        Timber.e(">>> Crop error msg : %s", error.getMessage());
                }
            }
            break;

            case UCrop.REQUEST_CROP: {
                Uri croppedImageUri = UCrop.getOutput(data);
                if (croppedImageUri != null) {
                    Timber.i(">>> cropped path... %s", croppedImageUri.getPath());
                    App.get().saveLocalAvatar(croppedImageUri);

                    loadAvatarIfAny(croppedImageUri, avatarImageView);
                }
            }
            break;

            case REQUEST_CODE_PHONE_ALBUM:
                if (data != null && data.getData() != null)
                    ImageUtilsKt.cropImage(this, data.getData());
                break;

            case REQUEST_CODE_PHONE_CAMERA: {
                // retrieve the persisted file
                Uri capturedImageUri = ImageUtilsKt.getCapturedImageOutputUri(this, capturedImageName);
                ImageUtilsKt.cropImage(this, capturedImageUri);
            }
            break;
            default:
                break;
        }

    }

    private void loadAvatarIfAny(Uri imageUri, ImageView imageView) {
        Glide.with(this).load(imageUri).into(imageView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_STORAGE_WRITE_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPickImage();
                }
                break;
            case REQUEST_CAMERA_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestTakeShot();
                }
                break;
        }
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(this, null, articleRepository);
        viewModel = new ViewModelProvider(this, factory).get(ArticleListViewModel.class);

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
                            Menu menu = navigationView.getMenu();

                            if (oldItemRefList != null && !oldItemRefList.isEmpty()
                                    && oldItemRefList.get(0) == oldItemRefs.get(0)) {
                                if (menu.size() > 0) {
                                    Timber.i(">>> menu items already up-to-date, update the selection now");
                                    setMenuItemCheckStatus(menu, true);
                                }
                                return;
                            }

                            oldItemRefList = oldItemRefs;
                            if (oldItemRefList == null || oldItemRefList.isEmpty()) {
                                return;
                            }


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

        if (selectedItemId == MENU_ID_BASE) {
            viewModel.load(false);
        } else {
            // FIXME: the detailed path should be hidden
            int id = keyValueStorage.getInt(LATEST_ISSUE_ID, 0);
            int selected = id - (selectedItemId - MENU_ID_BASE);

            viewModel.loadBy("/issues/issue-" + selected);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_theme, menu);
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
            case R.id.action_theme_day:
                App.get().updateTheme(ThemeOption.Day, true);
                break;
            case R.id.action_theme_night:
                App.get().updateTheme(ThemeOption.Night, true);
                break;
            case R.id.action_theme_sys:
                App.get().updateTheme(ThemeOption.System, true);
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
