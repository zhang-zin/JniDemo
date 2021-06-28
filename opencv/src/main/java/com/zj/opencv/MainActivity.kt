package com.zj.opencv

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.simpleName

    private val baseApi = TessBaseAPI()
    lateinit var tesstext: TextView
    lateinit var idCard: ImageView

    private var progressDialog: ProgressDialog? = null
    private var resultImage: Bitmap? = null
    private var fullImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tesstext = findViewById(R.id.tesstext)
        idCard = findViewById(R.id.idcard)
        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        initTess()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && data != null) {
            getResult(data.data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        baseApi.end()
    }

    private fun initTess() {
        HiExecutor.execute(runnable = object : HiExecutor.Callable<Boolean>() {
            override fun onPrepare() {
                showProgress()
            }

            override fun onBackground(): Boolean {
                val open = assets.open("cn.traineddata")
                val parentPath = getExternalFilesDir("")?.absolutePath
                        ?: Environment.getExternalStorageDirectory().absolutePath
                val file = File("$parentPath/tess/tessdata", "cn.traineddata")
                if (!file.exists()) {
                    file.createNewFile()
                    var fos: FileOutputStream? = null
                    try {
                        fos = FileOutputStream(file)
                        val buffer = ByteArray(2048)
                        var len: Int
                        while (open.read(buffer).also { len = it } != -1) {
                            fos.write(buffer, 0, len)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        fos?.close()
                        open.close()
                    }
                }
                if (!file.exists()) {
                    return false
                }
                return baseApi.init(file.parentFile!!.parentFile!!.absolutePath, "cn")
            }

            override fun onCompleted(t: Boolean) {
                dismissProgress()
                if (!t) {
                    Toast.makeText(this@MainActivity, "load trainedData failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun getResult(uri: Uri?) {
        uri?.run {
            val imagePath = if ("file" == uri.scheme) {
                Log.i(TAG, "获得图片：" + uri.path)
                uri.path
            } else if ("content" == uri.scheme) {
                val filePathColumns = arrayOf(MediaStore.Images.Media.DATA)
                val query = contentResolver.query(uri, filePathColumns, null, null, null)
                if (query?.moveToFirst() == true) {
                    val columnIndex = query.getColumnIndex(filePathColumns[0])
                    val image = query.getString(columnIndex)
                    query.close()
                    image
                } else {
                    ""
                }
            } else {
                ""
            }

            if (imagePath?.isNotEmpty() == true) {
                fullImage?.recycle()
                fullImage = toBitmap(imagePath)
                tesstext.text = null
                idCard.setImageBitmap(fullImage)
            }
        }
    }

    private fun toBitmap(imagePath: String): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)
        var widthTemp: Int = options.outWidth
        var heightTemp: Int = options.outHeight
        var scale = 1
        while (true) {
            if (widthTemp <= 640 && heightTemp <= 480) {
                break
            }
            widthTemp /= 2
            heightTemp /= 2
            scale *= 2
        }
        val opts: BitmapFactory.Options = BitmapFactory.Options()
        opts.inSampleSize = scale
        opts.outHeight = widthTemp
        opts.outWidth = heightTemp
        return BitmapFactory.decodeFile(imagePath, opts)
    }

    private fun showProgress() {
        if (progressDialog != null) {
            progressDialog!!.show()
        } else {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setMessage("请稍后")
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setCancelable(false);
            progressDialog!!.show()
        }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
    }

    /**
     * 选取身份证照片
     */
    fun search(view: View) {
        // sdk < Build.VERSION_CODES.KITKAT
        //val intent = Intent()
        //intent.setAction(Intent.ACTION_GET_CONTENT)
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.setType("image/*")
        startActivityForResult(Intent.createChooser(intent, "选择待识别图片"), 100)
    }

    /**
     * 查找id
     */
    fun searchId(view: View) {
        tesstext.setText(null)
        resultImage = null
        val bitmapResult = ImageProcess.getIdNumber(fullImage, Bitmap.Config.ARGB_8888)
        fullImage?.recycle()
        resultImage = bitmapResult
        idCard.setImageBitmap(resultImage)
    }

    /**
     * 识别文字
     */
    fun recognition(view: View) {
        baseApi.setImage(resultImage)
        tesstext.text = baseApi.utF8Text
        baseApi.clear()
    }
}