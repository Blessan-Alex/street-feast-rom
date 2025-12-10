package com.streatfeast.app.utils

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController

/**
 * Safe navigation helper to avoid IllegalArgumentException when navigating
 * from stale destinations.
 */
fun NavController.navigateSafe(@IdRes resId: Int, args: Bundle? = null) {
    val action = currentDestination?.getAction(resId)
    val destinationId = action?.destinationId ?: resId
    val hasDestination = graph.findNode(destinationId) != null
    if (hasDestination) {
        try {
            navigate(resId, args)
        } catch (_: IllegalArgumentException) {
            // Ignore if navigation is not valid from current destination
        }
    }
}

