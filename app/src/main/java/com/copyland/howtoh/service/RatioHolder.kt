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

    private var ratio:Float = 1.0f

    public fun setRatio(r :Float){
        ratio = r
    }

    public fun getRatio():Float{
        return ratio
    }
}