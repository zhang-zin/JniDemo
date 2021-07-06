package com.zj.opencv

import android.graphics.Bitmap

object ImageProcess {
    init {
        System.loadLibrary("native-lib")
    }

    external fun getIdNumber(src: Bitmap?, config: Bitmap.Config?): Bitmap?
}