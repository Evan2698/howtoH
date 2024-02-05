package com.copyland.howtoh.service

class RatioHolder {
    companion object {
        @Volatile
        private var holder: RatioHolder? = null

        fun getInstance(): RatioHolder {
            if (holder == null) {
                synchronized(this) {
                    if (holder == null) {
                        holder = RatioHolder()
                    }
                }
            }
            return holder!!
        }
    }

    var realWidth: Double = 1.0

    var realHeight: Double = 1.0

    var screenWidth: Double = 1.0
    var screenHeight: Double = 1.0
}