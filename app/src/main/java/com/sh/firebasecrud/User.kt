package com.sh.firebasecrud

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User (
    var id: String? = "",
    var username: String? = "",
    var email: String? = "",
    var password: String? = "",
    var avatarPath: String? = ""
) : Parcelable