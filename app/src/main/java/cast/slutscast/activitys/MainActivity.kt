package cast.slutscast.activitys

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import androidx.mediarouter.app.MediaRouteButton
import cast.slutscast.R
import cast.slutscast.fragments.LoaderFragment
import com.google.android.gms.cast.framework.*
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var toolBar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem
    private lateinit var mSessionManager: SessionManager
    private lateinit var mCastContext: CastContext
    private lateinit var mMediaRouteButton: MediaRouteButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMediaRouteButton = findViewById(R.id.media_route_button)
        CastButtonFactory.setUpMediaRouteButton(this, mMediaRouteButton)
        mCastContext = CastContext.getSharedInstance(this)
        mSessionManager = CastContext.getSharedInstance(this).sessionManager
            .apply {
                toolBar = findViewById(R.id.toolBar)
                progressBar = findViewById(R.id.progressBar)
                drawerLayout = findViewById(R.id.drawerLayout)
                navigationView = findViewById(R.id.navigationView)
            }
        setSupportActionBar(toolBar).apply {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        drawerLayout.apply { addDrawerListener(
            ActionBarDrawerToggle(this@MainActivity, drawerLayout, toolBar,0,0).apply
            {
                syncState()
                drawerArrowDrawable.color = ContextCompat.getColor(this@MainActivity, R.color.white) })
            isClickable = true
        }
        navigationView.apply { itemIconTintList = null }.setNavigationItemSelectedListener(this)
        setActionBarTitle("Featured Cams", "on Chaturbate")
        writeToast("Open Featured Cams")
        handleIntent(intent)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.favorite -> {}
            R.id.chaturbate_featured -> {replaceFragment("https://chaturbate.com/?")
                setActionBarTitle("Featured Cams", "on Chaturbate")
                writeToast("Open Featured Cams")
            }
            R.id.chaturbate_female -> {replaceFragment("https://chaturbate.com/female-cams/?")
                setActionBarTitle("Female Cams", "on Chaturbate")
                writeToast("Open Female Cams")
            }
            R.id.chaturbate_male -> {replaceFragment("https://chaturbate.com/male-cams/?")
                setActionBarTitle("Male Cams", "on Chaturbate")
                writeToast("Open Male Cams")
            }
            R.id.chaturbate_couple -> {replaceFragment("https://chaturbate.com/couple-cams/?")
                setActionBarTitle("Couple Cams", "on Chaturbate")
                writeToast("Open Couple Cams")
            }
            R.id.chaturbate_trans -> {replaceFragment("https://chaturbate.com/trans-cams/?")
                setActionBarTitle("Trans Cams", "on Chaturbate")
                writeToast("Open Trans Cams")
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                writeToast("Open Settings")
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun writeToast(message:String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setActionBarTitle(title:String, subtitle:String){
        supportActionBar?.apply {
            setTitle(title)
            setSubtitle(subtitle)
        }
    }

    private fun replaceFragment(url:String){
        supportFragmentManager.commit{
            replace(R.id.fragmentContainerView, LoaderFragment.newInstance(url))
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)

        searchItem = menu.findItem(R.id.app_bar_search)
        searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnCloseListener { true }

        val searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
        searchPlate.hint = "Suche..."
        val searchPlateView: View = searchView.findViewById(androidx.appcompat.R.id.search_plate)
        searchPlateView.setBackgroundColor(
            ContextCompat.getColor(this,android.R.color.transparent)
        )

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                replaceFragment("https://chaturbate.com/?keywords=$query&")
                setActionBarTitle(query,"")
                writeToast("Open $query")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            replaceFragment("https://chaturbate.com/?keywords=${query.toString()}&")
            setActionBarTitle(query.toString(),"")
            writeToast("Open " + query.toString())
            //MenuItemCompat.collapseActionView(findViewById(R.id.app_bar_search))
        }
    }

}