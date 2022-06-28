package com.example.microheadphone

import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat() {

    var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private var mMediaPlayer = MediaPlayer()
    private var isPaused = false
    private var shouldWait = false
    private var isDoubleClicked = false
    private var recordIndex = 0
    private var records: List<String> = listOf()

    private val mediaSessionCompatCallBack: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            mMediaPlayer.start()
            isPaused = false
            Toast.makeText(application, "Play Button is pressed!", Toast.LENGTH_SHORT).show()
        }

        override fun onPause() {
            super.onPause()
            mMediaPlayer.pause()
            isPaused = true
            Toast.makeText(application, "Pause Button is pressed!", Toast.LENGTH_SHORT).show()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()

            recordIndex += 1
            if (recordIndex >= records.size) {
                recordIndex = 0
            }

            mMediaPlayer.release()
            mMediaPlayer = MediaPlayer()
            mMediaPlayer.setDataSource(records[recordIndex])
            mMediaPlayer.setOnCompletionListener { onSkipToNext() }
            mMediaPlayer.prepare()
            mMediaPlayer.start()
            Toast.makeText(application, "Next Button is pressed!", Toast.LENGTH_SHORT).show()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()

            recordIndex -= 1
            if (recordIndex <= 0) {
                recordIndex = records.size - 1
            }

            mMediaPlayer.release()
            mMediaPlayer = MediaPlayer()
            mMediaPlayer.setDataSource(records[recordIndex])
            mMediaPlayer.setOnCompletionListener { onSkipToNext() }
            mMediaPlayer.prepare()
            mMediaPlayer.start()
            Toast.makeText(application, "Previous Button is pressed!", Toast.LENGTH_SHORT).show()
        }

        override fun onStop() {
            super.onStop()
            Toast.makeText(application, "Stop Button is pressed!", Toast.LENGTH_SHORT).show()
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            Toast.makeText(application, "Stop Button is pressed!", Toast.LENGTH_SHORT).show()
            super.onCommand(command, extras, cb)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Toast.makeText(application, "Stop Button is pressed!", Toast.LENGTH_SHORT).show()
            super.onPlayFromMediaId(mediaId, extras)
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val intentAction = mediaButtonEvent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                val event: KeyEvent? = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                if (event != null) {
                    val action: Int = event.action
                    if (action == KeyEvent.ACTION_DOWN) {
                        when (event.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                onSkipToNext()
                                isPaused = false
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                if (shouldWait) {
                                    isDoubleClicked = true
                                    return true
                                }
                                shouldWait = true
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isDoubleClicked) {
                                        onSkipToPrevious()
                                        isPaused = false
                                    } else {
                                        if (isPaused) {
                                            onPlay()
                                        } else {
                                            onPause()
                                        }
                                    }
                                    shouldWait = false
                                    isDoubleClicked = false
                                }, 1000)
                                return true
                            }
                            KeyEvent.KEYCODE_HEADSETHOOK -> {
                                if (shouldWait) {
                                    isDoubleClicked = true
                                    return true
                                }
                                shouldWait = true
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isDoubleClicked) {
                                        onSkipToNext()
                                        isPaused = false
                                    } else {
                                        if (isPaused) {
                                            onPlay()
                                        } else {
                                            onPause()
                                        }
                                    }
                                    shouldWait = false
                                    isDoubleClicked = false
                                }, 1000)
                                return true
                            }
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> // code for fast forward
                                return true
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> // code for previous
                                return true
                            KeyEvent.KEYCODE_MEDIA_REWIND -> // code for rewind
                                return true
                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                // code for stop
                                Toast.makeText(
                                    application,
                                    "Stop Button is pressed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return true
                            }
                        }
                        return false
                    }
                    if (action == KeyEvent.ACTION_UP) {
                    }
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        records = intent?.getStringArrayListExtra("records").orEmpty()
        records = (records as MutableList).sortedBy { it.split('/').last() }
        recordIndex = 0
        mMediaPlayer.setDataSource(records[recordIndex])
        mMediaPlayer.setOnCompletionListener { mediaSessionCompatCallBack.onSkipToNext() }
        mMediaPlayer.prepare()
        mMediaPlayer.start()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, "MEDIA_BROWSER_SERVICE").apply {

            // Enable callbacks from MediaButtons and TransportControls
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1F, SystemClock.elapsedRealtime())
            setPlaybackState(stateBuilder.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(mediaSessionCompatCallBack)

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
            setMediaButtonReceiver(PendingIntent.getBroadcast(this@MediaPlaybackService, 0, Intent(this@MediaPlaybackService, MediaButtonReceiver::class.java), 0))
            isActive = true
        }
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot {

        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        //  Browsing not allowed
        if (MY_EMPTY_MEDIA_ROOT_ID == p0) {
            p1.sendResult(null)
            return
        }

        // Assume for example that the music catalog is already loaded/cached.

        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>()

        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID == p0) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        p1.sendResult(mediaItems.toMutableList())
    }
}
