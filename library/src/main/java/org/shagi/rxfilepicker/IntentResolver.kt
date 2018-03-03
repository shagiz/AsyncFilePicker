package org.shagi.rxfilepicker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import java.io.File
import java.util.*

class IntentResolver(private val activity: Activity) {

    private var galleryIntent: Intent? = null
    private var cameraIntent: Intent? = null
    private var saveFile: File? = null

    private fun loadSystemPackages(intent: Intent): Intent {
        val resInfo = activity.packageManager.queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY)

        if (!resInfo.isEmpty()) {
            val packageName = resInfo[0].activityInfo.packageName
            intent.`package` = packageName
        }

        return intent
    }

    private fun getCameraIntent(): Intent {
        if (cameraIntent == null) {
            cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent?.putExtra(MediaStore.EXTRA_OUTPUT, cameraUriForProvider())

            applyProviderPermission()
        }

        return cameraIntent ?: throw IllegalStateException()
    }

    /**
     * Granting permissions to write and read for available cameras to file provider.
     */
    private fun applyProviderPermission() {
        val resInfoList = activity.packageManager.queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            activity.grantUriPermission(packageName, cameraUriForProvider(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun cameraFile(): File {
        if (saveFile == null) {
            val directory = File(activity.filesDir, "picked")
            directory.mkdirs()
            saveFile = File(directory, "current")
        }

        return saveFile ?: throw IllegalStateException("file have to be created")
    }

    fun cameraUri(): Uri {
        return Uri.fromFile(cameraFile())
    }

    private fun getAuthority(): String {
        return activity.application.packageName + ".provider"
    }

    private fun cameraUriForProvider(): Uri {
        try {
            return FileProvider.getUriForFile(activity, getAuthority(), cameraFile())
        } catch (e: Exception) {
            if (e.message?.contains("ProviderInfo.loadXmlMetaData") == true) {
                throw Error("wrong_authority")
            } else {
                throw e
            }
        }
    }

    private fun getGalleryIntent(): Intent {
        if (galleryIntent == null) {
            galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryIntent?.type = "image/*"
        }

        return galleryIntent ?: throw IllegalStateException("intent must be created")
    }

    fun launchCamera(listener: Fragment) {
        if (requestPermissions(listener, getCameraPermissions()) && getCameraIntent().resolveActivity(activity.packageManager) != null) {
            listener.startActivityForResult(loadSystemPackages(getCameraIntent()), REQUESTER)
        }
    }

    fun launchGallery(listener: Fragment) {
        if (requestPermissions(listener, getGalleryCameraPermissions())) {
            listener.startActivityForResult(loadSystemPackages(getGalleryIntent()), REQUESTER)
        }
    }

    private fun getCameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun getGalleryCameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun requestPermissions(listener: Fragment, permissions: Array<String>): Boolean {
        val list = ArrayList<String>()

        for (permission in permissions)
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED)
                list.add(permission)

        if (list.isEmpty())
            return true

        listener.requestPermissions(list.toTypedArray(), REQUESTER)
        return false
    }

    fun fromCamera(data: Intent?): Boolean {
        return data == null || data.data == null || data.data.toString().contains(cameraFile().toString())
    }

    fun getActivity(): Activity {
        return activity
    }

    companion object {
        const val REQUESTER = 99
    }
}