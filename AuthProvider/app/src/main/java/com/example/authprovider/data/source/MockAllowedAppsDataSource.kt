package com.example.authprovider.data.source

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAllowedAppsDataSource @Inject constructor() : AllowedAppsDataSource {

    private val allowedPackages = listOf(
        "com.example.authconsumer"
    )

    override fun getAllowedPackages(): List<String> = allowedPackages

    override fun isPackageAllowed(packageName: String): Boolean {
        return packageName in allowedPackages
    }
}
