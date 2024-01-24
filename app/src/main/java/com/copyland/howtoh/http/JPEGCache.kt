package com.copyland.howtoh.http

import java.io.ByteArrayOutputStream

interface JPEGCache {
    //get a jpeg picture from stream
    fun takeImageFromStream(): ByteArrayOutputStream
}