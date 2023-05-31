package com.rayworks.droidweekly.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.rayworks.droidweekly.BuildConfig
import com.yalantis.ucrop.UCrop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

const val ONE_MEGA_BYTES = 1024 * 1024

/***
 * Request to crop the target image.
 * See also [this article](https://medium.com/@arkapp/accessing-images-on-android-10-scoped-storage-bbe65160c3f4)
 */
fun Activity.cropImage(imageUri: Uri) {
    val selectedBitmap: Bitmap? = getBitmap(this, imageUri)
    val selectedImgFile = File(
        getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        getTimestamp().toString() + "_selectedImg.jpg",
    )
    convertBitmapToFile(selectedImgFile, selectedBitmap!!)

    val croppedImgFile = File(
        getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        getTimestamp().toString() + "_croppedImg.jpg",
    )
    startCrop(this, Uri.fromFile(selectedImgFile), Uri.fromFile(croppedImgFile))
}

fun getFileUri(context: Context, pkg: String, file: File): Uri {
    val auth = "$pkg.fileprovider"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, auth, file)
    } else {
        Uri.fromFile(file)
    }
}

fun getTimestamp(): Long {
    return System.currentTimeMillis()
}

private fun startCrop(context: Activity, sourceUri: Uri, destinationUri: Uri) {
    val options = UCrop.Options().also {
        it.setHideBottomControls(true)
        it.withMaxResultSize(480, 480)
        it.setMaxBitmapSize(ONE_MEGA_BYTES)
    }

    UCrop.of(sourceUri, destinationUri)
        .withAspectRatio(1f, 1f)
        .withOptions(options)
        .start(context)
}

fun convertBitmapToFile(destinationFile: File, bitmap: Bitmap) {
    // create a file to write bitmap data
    destinationFile.createNewFile()

    // Convert bitmap to byte array
    val bos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos)
    val bitmapData = bos.toByteArray()
    // write the bytes in file
    val fos = FileOutputStream(destinationFile)
    fos.write(bitmapData)
    fos.flush()
    fos.close()
}

fun getBitmap(context: Context, imageUri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, imageUri),
        )
    } else {
        context.contentResolver.openInputStream(imageUri)?.let {
            return BitmapFactory.decodeStream(it)
        }
        return null
    }
}

fun getAuthority(): String {
    return BuildConfig.APPLICATION_ID + ".fileprovider"
}

fun getCapturedImageOutputUri(context: Context, imageName: String): Uri {
    val capturedImgFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageName)

    return FileProvider.getUriForFile(context, getAuthority(), capturedImgFile)
}
