package com.example.authprovider.data.source

interface AllowedAppsDataSource {
    fun getAllowedPackages(): List<String>
    fun isPackageAllowed(packageName: String): Boolean
}
