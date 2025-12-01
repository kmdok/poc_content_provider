package com.example.authprovider.provider

import android.net.Uri

object AuthKeyContract {
    const val AUTHORITY = "com.example.authprovider"
    const val PATH_AUTHKEYS = "authkeys"
    const val PATH_CURRENT = "current"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")
    val CONTENT_URI_AUTHKEYS: Uri = Uri.parse("content://$AUTHORITY/$PATH_AUTHKEYS")
    val CONTENT_URI_CURRENT: Uri = Uri.parse("content://$AUTHORITY/$PATH_CURRENT")

    object Columns {
        const val ID = "id"
        const val KEY = "key"
        const val CREATED_AT = "created_at"
        const val EXPIRES_AT = "expires_at"
        const val IS_EXPIRED = "is_expired"
    }
}
