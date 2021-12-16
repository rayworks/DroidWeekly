package com.rayworks.droidweekly.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.rayworks.droidweekly.App
import com.rayworks.droidweekly.R
import com.rayworks.droidweekly.databinding.ActivityMainBinding
import com.rayworks.droidweekly.di.KeyValueStorage
import com.rayworks.droidweekly.model.OldItemRef
import com.rayworks.droidweekly.model.ThemeOption
import com.rayworks.droidweekly.repository.LATEST_ISSUE_ID
import com.rayworks.droidweekly.search.SearchActivity.Companion.launch
import com.rayworks.droidweekly.ui.component.ArticleAdapter
import com.rayworks.droidweekly.utils.cropImage
import com.rayworks.droidweekly.utils.getCapturedImageOutputUri
import com.rayworks.droidweekly.viewmodel.ArticleListViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/***
 * The main dash board
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 0xF1
    private val REQUEST_CAMERA_ACCESS_PERMISSION = 0xF2
    private val REQUEST_CODE_PHONE_ALBUM = 0xE1
    private val REQUEST_CODE_PHONE_CAMERA = 0xE2
    private val capturedImageName = "aw_captured_img.png"
    private val MENU_ID_BASE = Menu.FIRST + 0xFF

    val viewModel: ArticleListViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var oldItemRefList: List<OldItemRef>? = null
    private var selectedItemId = MENU_ID_BASE
    private lateinit var headerView: View
    private var avatarImageView: ImageView? = null

    @JvmField
    @Inject
    var keyValueStorage: KeyValueStorage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_24dp)
        setupNavigationDrawer()
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { launch(this@MainActivity) }

        setupArticleList()
        setupViewModel()
        val lastSelected = viewModel.selectedItemId
        if (lastSelected > 0) {
            selectedItemId = lastSelected
        }
    }

    private fun setupArticleList() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_list)
        val articleAdapter = ArticleAdapter(this, ArrayList())
        articleAdapter.setViewArticleListener { url: String? ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        recyclerView.adapter = articleAdapter
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, layoutManager.orientation)
        )
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        headerView = LayoutInflater.from(this)
            .inflate(R.layout.view_layout_nav_header, navigationView, false)
        avatarImageView = headerView.findViewById(R.id.avatar)
        avatarImageView?.setOnClickListener { promptPickingImage() }
        val localAvatar = App.get().localAvatar
        if (!TextUtils.isEmpty(localAvatar)) {
            loadAvatarIfAny(Uri.parse(localAvatar), avatarImageView)
        }
        navigationView.addHeaderView(headerView)
        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            val id = menuItem.itemId
            selectedItemId = id
            viewModel.selectedItemId = id
            if (oldItemRefList != null) {
                val (_, relativePath) = oldItemRefList!![id - MENU_ID_BASE]
                viewModel.loadBy(relativePath)
                menuItem.isChecked = true
                drawerLayout.closeDrawers()
            }
            true
        }
    }

    private fun promptPickingImage() {
        val popupMenu = PopupMenu(this, headerView, Gravity.CENTER_HORIZONTAL)
        val inflater = popupMenu.menuInflater
        val menu = popupMenu.menu
        inflater.inflate(R.menu.menu_image_selection, menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.from_album) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_WRITE_ACCESS_PERMISSION
                    )
                } else {
                    requestPickImage()
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA_ACCESS_PERMISSION
                    )
                } else {
                    requestTakeShot()
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun requestTakeShot() {
        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        intent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            getCapturedImageOutputUri(this, capturedImageName)
        )
        startActivityForResult(intent, REQUEST_CODE_PHONE_CAMERA)
    }

    private fun requestPickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PHONE_ALBUM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            Timber.w(">>> resultCode : \$resultCode for original request \$requestCode with data : \$data")
            return
        }
        when (requestCode) {
            UCrop.RESULT_ERROR -> {
                if (data != null) {
                    val error = UCrop.getError(data)
                    if (error != null) Timber.e(">>> Crop error msg : %s", error.message)
                }
            }
            UCrop.REQUEST_CROP -> {
                val croppedImageUri = UCrop.getOutput(data!!)
                if (croppedImageUri != null) {
                    Timber.i(">>> cropped path... %s", croppedImageUri.path)
                    App.get().saveLocalAvatar(croppedImageUri)
                    loadAvatarIfAny(croppedImageUri, avatarImageView)
                }
            }
            REQUEST_CODE_PHONE_ALBUM -> if (data != null && data.data != null) this.cropImage(
                data.data!!
            )
            REQUEST_CODE_PHONE_CAMERA -> {

                // retrieve the persisted file
                val capturedImageUri = getCapturedImageOutputUri(this, capturedImageName)
                this.cropImage(capturedImageUri)
            }
            else -> {
            }
        }
    }

    private fun loadAvatarIfAny(imageUri: Uri, imageView: ImageView?) {
        Glide.with(this).load(imageUri).into(imageView!!)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_WRITE_ACCESS_PERMISSION -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPickImage()
            }
            REQUEST_CAMERA_ACCESS_PERMISSION -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestTakeShot()
            }
        }
    }

    private fun setupViewModel() {

        val dataBinding = ActivityMainBinding.bind(findViewById(R.id.drawer_layout))
        dataBinding.viewmodel = viewModel

        // enables MutableLiveData to be update on your UI
        dataBinding.lifecycleOwner = this
        viewModel.articleLoaded.addOnPropertyChangedCallback(
            object : OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                    if (!viewModel.articleLoaded.get()) {

                        // reset for next round
                        viewModel.articleLoaded.set(true)
                        val toast = Toast.makeText(
                            this@MainActivity,
                            "Failed to load content, please try again late",
                            Toast.LENGTH_SHORT
                        )
                        toast.setGravity(Gravity.CENTER, 0, 0)
                        toast.show()
                    }
                }
            })
        viewModel.itemRefs.observe(this, { oldItemRefs: List<OldItemRef> ->
            val menu = navigationView.menu
            if (oldItemRefList != null && !oldItemRefList!!.isEmpty() &&
                oldItemRefList!![0] === oldItemRefs[0]
            ) {
                if (menu.size() > 0) {
                    Timber.i(">>> menu items already up-to-date, update the selection now")
                    setMenuItemCheckStatus(menu, true)
                }
                return@observe
            }
            oldItemRefList = oldItemRefs
            if (oldItemRefList == null || oldItemRefList!!.isEmpty()) {
                return@observe
            }
            menu.clear()
            var base = MENU_ID_BASE
            for ((title) in oldItemRefList!!) {
                val pos = base++
                menu.add(0, pos, pos, title)
            }

            // set exclusive item check
            menu.setGroupCheckable(0, true, true)

            // default selection
            setMenuItemCheckStatus(menu, true)
            navigationView.invalidate()
        })
    }

    private fun setMenuItemCheckStatus(menu: Menu, checked: Boolean) {
        if (selectedItemId > 0) {
            menu.getItem(selectedItemId - MENU_ID_BASE).isChecked = checked
        }
    }

    override fun onResume() {
        super.onResume()
        if (selectedItemId == MENU_ID_BASE) {
            viewModel.load(false)
        } else {
            // FIXME: the detailed path should be hidden
            val id = keyValueStorage!!.getInt(LATEST_ISSUE_ID, 0)
            val selected = id - (selectedItemId - MENU_ID_BASE)
            viewModel.loadBy("/issues/issue-$selected")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_theme, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        when (id) {
            R.id.action_theme_day -> App.get().updateTheme(ThemeOption.Day, true)
            R.id.action_theme_night -> App.get().updateTheme(ThemeOption.Night, true)
            R.id.action_theme_sys -> App.get().updateTheme(ThemeOption.System, true)
            android.R.id.home -> drawerLayout.openDrawer(GravityCompat.START)
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
