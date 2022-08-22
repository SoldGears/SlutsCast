package cast.slutscast.activitys

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cast.slutscast.R
import cast.slutscast.casts.ExpandedControlsActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import android.widget.SeekBar.OnSeekBarChangeListener as OnSeekBarChangeListener
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder as TrackSelectionDialogBuilder

private var HLS_STATIC_URL = ""
const val STATE_RESUME_WINDOW = "resumeWindow"
const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
const val STATE_PLAYER_PLAYING = "playerOnPlay"
const val MAX_HEIGHT = 539
const val MAX_WIDTH = 959

class DetailActivity : AppCompatActivity() {
    private var mCastSession: CastSession? = null
    private var mCastContext: CastContext? = null
    private lateinit var mSessionManager: SessionManager
    private val mSessionManagerListener: SessionManagerListenerImpl = SessionManagerListenerImpl()

    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var playerView: PlayerView
    private lateinit var exoQuality: ImageButton
    private lateinit var mainFrameLayout: FrameLayout

    private lateinit var progressBar: ProgressBar
    private lateinit var exoPlay:ImageButton
    private lateinit var exoFullScreenIcon: ImageView
    private lateinit var btFullscreen: FrameLayout
    private lateinit var mediaItem:MediaItem
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var fullscreenDialog: Dialog? = null
    private var isFullscreen = false
    private var isPlayerPlaying = true
    private var trackDialog: Dialog? = null
    private var modelName:String = ""

    //VOLUME
    private lateinit var audioManager: AudioManager
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var volumeMute:ImageButton
    private var currentVolume:Int = 0
    private var minimumVolume:Int = 0
    private var maximumVolume:Int = 150

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        mCastContext = CastContext.getSharedInstance(this)
        mSessionManager = CastContext.getSharedInstance(this).sessionManager
        HLS_STATIC_URL = this.intent.getStringExtra("link").toString()
        modelName = this.intent.getStringExtra("title").toString()
        setSupportActionBar(findViewById(R.id.toolBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
            .apply { title = modelName }

        playerView = findViewById(R.id.playerView)
        exoPlay = playerView.findViewById(R.id.exo_play)
        exoQuality = playerView.findViewById(R.id.exo_quality)
        progressBar = playerView.findViewById(R.id.progressBar)
        exoFullScreenIcon = playerView.findViewById(R.id.exo_fullscreen_icon)
        btFullscreen = playerView.findViewById(R.id.exo_fullscreen)

        initFullScreenDialog()
        initFullScreenButton()

        dataSourceFactory = DefaultDataSourceFactory(this,
            Util.getUserAgent(this, R.string.app_name.toString()))

        exoQuality.setOnClickListener{
            if(trackDialog == null){
                initPopupQuality()
            }
            trackDialog?.show()
        }

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            //  playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
            isPlayerPlaying = savedInstanceState.getBoolean(STATE_PLAYER_PLAYING)
        }

        mediaItem = MediaItem.Builder()
            .setUri(HLS_STATIC_URL)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .build()

        if (isCastConnected()){
            initPlayer(false)
            startPlaybackOnChromecast(modelName, "Presents from SlutsCast.", HLS_STATIC_URL, "")
        }else{
            initPlayer(true)
        }

    }

    private fun soundHandle(){
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volumeSeekBar = findViewById(R.id.volume_seekbar)
        maximumVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        minimumVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        // currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                currentVolume = volumeSeekBar.progress
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_SHOW_UI)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
    }
    
    private fun initPlayer(starting:Boolean){
        trackSelector = DefaultTrackSelector(this)
        trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSize(MAX_WIDTH, MAX_HEIGHT))
        exoPlayer = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build().apply {
            playWhenReady = isPlayerPlaying
            //   seekTo(currentWindow, playbackPosition)
            setMediaItem(mediaItem)
            prepare()
            if (starting){
                play()
            }

        }

        exoPlayer.addListener(object : Player.Listener {
            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        progressBar.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        progressBar.visibility = View.GONE
                        exoQuality.visibility = View.VISIBLE
                    }
                    ExoPlayer.STATE_IDLE -> {
                        //   exoPlay.visibility = View.VISIBLE
                        exoPlay.visibility = View.VISIBLE
                    }
                }
            }
        })
        playerView.setOnContextClickListener(object : Player.Listener, View.OnContextClickListener {
            override fun onContextClick(p0: View?): Boolean {
                TODO("Not yet implemented")
                if (exoPlayer.isPlaying) exoPlay.visibility = View.GONE else exoPlay.visibility = View.VISIBLE
            }

        })

        playerView.player = exoPlayer

        if (isFullscreen) {
            openFullscreenDialog()
        }

    }
    //
    private fun startPlaybackOnChromecast(title:String, subtitle:String, link: String, preview:String) {
        if (mCastContext == null) return
        // var mediaMetadata: MediaMetadata? = null
        //         mediaMetadata?.putString(MediaMetadata.KEY_TITLE, title)

        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, title)
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle)
        //  mediaMetadata?.putString(MediaMetadata.KEY_SUBTITLE, subtitle)
        Log.i("Chromecast", "use image: $preview")
        if (TextUtils.isEmpty(preview)){
            mediaMetadata.addImage(WebImage(Uri.parse("https://kinoca.st/img/kinocast_icon_512.png")))
        }else{
            mediaMetadata.addImage(WebImage(Uri.parse(preview)))
        }
        Log.i("cast", "play $link")
        val mediaInfo = MediaInfo.Builder(link)
            //.setContentType("video/mp4")
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setMetadata(mediaMetadata)
            .build()
        mCastContext!!.sessionManager
            .currentCastSession
            ?.remoteMediaClient
            ?.load(mediaInfo, true)
    }

    private fun isCastConnected(): Boolean {
        if (mCastContext == null) return false
        val castSession = mCastContext!!
            .sessionManager
            .currentCastSession
        return castSession != null && castSession.isConnected
    }
    //
    private fun loadRemoteMedia(position: Int, autoPlay: Boolean) {
        val remoteMediaClient = mCastSession?.remoteMediaClient ?: return

        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val intent = Intent(this@DetailActivity, ExpandedControlsActivity::class.java)
                startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
            }
        })
        class MyActivity : FragmentActivity() {
            @SuppressLint("RestrictedApi")
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                return (CastContext.getSharedInstance(this)
                    .onDispatchVolumeKeyEventBeforeJellyBean(event)
                        || super.dispatchKeyEvent(event))
            }
        }
        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                //  .setMediaInfo(mSelectedMedia)
                .setAutoplay(autoPlay)
                .setCurrentTime(position.toLong()).build()
        )
    }
    //
    private fun initPlayerControls(){
        volumeSeekBar = findViewById(R.id.volume_seekbar)
        volumeMute = findViewById(R.id.exo_mute)
        var mute:Boolean = false
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        // volumeMute.setOnClickListener(){
        //     if (!mute){
        //  mute = true
//                volumeSeekBar.visibility = View.
        // volumeMute.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.ic_volume_off_24))
        //     }else{
        // mute = false
        // volumeMute.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.ic_volume_up_24))
        //    }
        //    }

        volumeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                //  volumeSeekBar .setText("Media Volume : $i")
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)
                if (i > 0){
                    mute = true
                    volumeSeekBar.visibility = View.GONE
                    volumeMute.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.ic_volume_off_24))
                }
                volumeMute.setOnClickListener(){
                    if (mute){
                        mute = false
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0)
                        volumeSeekBar.max
                        volumeSeekBar.visibility = View.VISIBLE
                        volumeMute.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.ic_volume_up_24))                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
    //OVERRIDE FUN PARTS

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            if (isCastConnected()){
                initPlayer(false)
                startPlaybackOnChromecast(modelName, "Presents from SlutsCast.", HLS_STATIC_URL, "")
            }else{
                initPlayer(true)
            }
            playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) {
            if (isCastConnected()){
                initPlayer(false)
                startPlaybackOnChromecast(modelName, "Presents from SlutsCast.", HLS_STATIC_URL, "")
            }else{
                initPlayer(true)
            }
            playerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playerView.onPause()
            releasePlayer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, exoPlayer.currentWindowIndex)
      //  outState.putLong(STATE_RESUME_POSITION, exoPlayer.currentPosition)
        outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        outState.putBoolean(STATE_PLAYER_PLAYING, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        exoPlayer.release()
        super.onDestroy()
    }

    private fun releasePlayer(){
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        currentWindow = exoPlayer.currentWindowIndex
        exoPlayer.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.detail_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(
            applicationContext,
            menu,
            R.id.media_route_menu_item
        )
        return true
    }

    //QUALITY POPUP DIALOG PARTS
    private fun initPopupQuality() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        var videoRenderer : Int? = null

        if(mappedTrackInfo == null) return else exoQuality.visibility = View.VISIBLE

        for(i in 0 until mappedTrackInfo.rendererCount){
            if(isVideoRenderer(mappedTrackInfo, i)){
                videoRenderer = i
            }
        }

        if(videoRenderer == null){
            exoQuality.visibility = View.GONE
            return
        }

        val trackSelectionDialogBuilder = TrackSelectionDialogBuilder(this, getString(R.string.qualitySelector), trackSelector, videoRenderer)
        trackSelectionDialogBuilder.setTrackNameProvider{
            // Override function getTrackName
            getString(R.string.exo_track_resolution_pixel, it.height)
        }
        trackDialog = trackSelectionDialogBuilder.build()

    }
    private fun isVideoRenderer(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (trackGroupArray.length == 0) {
            return false
        }
        val trackType = mappedTrackInfo.getRendererType(rendererIndex)
        return C.TRACK_TYPE_VIDEO == trackType
    }

    // FULLSCREEN PART
    private fun initFullScreenDialog() {
        fullscreenDialog = object: Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            override fun onBackPressed() {
                if(isFullscreen) closeFullscreenDialog()
                super.onBackPressed()
            }
        }
    }
    private fun initFullScreenButton(){
        btFullscreen.setOnClickListener {
            if (!isFullscreen) {
                openFullscreenDialog()
            } else {
                closeFullscreenDialog()
            }
        }
    }
    @SuppressLint("SourceLockedOrientationActivity")
    private fun openFullscreenDialog() {
        exoFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_shrink))
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        (playerView.parent as ViewGroup).removeView(playerView)
        fullscreenDialog?.addContentView(playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        isFullscreen = true
        fullscreenDialog?.show()
    }
    private fun closeFullscreenDialog() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        (playerView.parent as ViewGroup).removeView(playerView)
        mainFrameLayout.addView(playerView)
        exoFullScreenIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_expand))
        isFullscreen = false
        fullscreenDialog?.dismiss()
    }

    //CHROMECAST PARTS
    inner class SessionManagerListenerImpl : SessionManagerListener<CastSession> {
        override fun onSessionEnding(p0: CastSession) {
            TODO("Not yet implemented")
        }
        override fun onSessionResumeFailed(p0: CastSession, p1: Int) {
            TODO("Not yet implemented")
        }
        override fun onSessionResuming(p0: CastSession, p1: String) {
            TODO("Not yet implemented")
        }
        override fun onSessionStartFailed(p0: CastSession, p1: Int) {
            TODO("Not yet implemented")
        }
        override fun onSessionStarting(p0: CastSession) {
            TODO("Not yet implemented")
        }
        override fun onSessionSuspended(p0: CastSession, p1: Int) {
            TODO("Not yet implemented")
        }
        override fun onSessionStarted(p0: CastSession, p1: String) {
            invalidateOptionsMenu()
        }
        override fun onSessionResumed(p0: CastSession, p1: Boolean) {
            invalidateOptionsMenu()
        }
        override fun onSessionEnded(p0: CastSession, p1: Int) {
            finish()
        }
    }

}