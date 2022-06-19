package com.example.microheadphone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.core.app.ActivityCompat

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
    }

    private fun startOurService() {
        val intent = Intent(this, MediaPlaybackService::class.java)
        val data = arrayListOf<String>()
        data.addAll(getRecords())
        intent.putStringArrayListExtra("records", data)
        startService(intent)
    }

    private fun getRecords(): MutableList<String> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
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
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val path = cursor.getString(audioIndex)

                    songs.add(path)
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()

        return songs
    }

    private fun isStoragePermissionGranted(): Boolean {
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
