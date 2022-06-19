package com.example.microheadphone

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.nio.file.spi.FileSystemProvider

class MainActivity : Activity() {
    private var sendBroadcastBtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendBroadcastBtn = findViewById(R.id.sendBroadcastBtn)
        sendBroadcastBtn?.setOnClickListener {
            startOurService()
        }
        isStoragePermissionGranted()
        startOurService()
//        playDummySound()
    }

    private fun startOurService() {
        val intent = Intent(this, MediaPlaybackService::class.java)
        val data = arrayListOf<String>()
        data.addAll(getRecords())
        intent.putStringArrayListExtra("records", data)
        startService(intent)
    }

    private fun playDummySound() {
        val mMediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.silent_sound)
        mMediaPlayer.setOnCompletionListener { mMediaPlayer.release() }
        mMediaPlayer.start()
        mMediaPlayer.release()
    }

    private fun getRecords(): MutableList<String> {
        FileSystemProvider.installedProviders()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH
        )

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        val songs: MutableList<String> = ArrayList()
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val audioIndex: Int =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
                    val path = cursor.getString(audioIndex)
                    val audioIndexName: Int =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val pathName = cursor.getString(audioIndexName)

                    songs.add(path + pathName)
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()

        return songs
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            false
        }
    }
}
