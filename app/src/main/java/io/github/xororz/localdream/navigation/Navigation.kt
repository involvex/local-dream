package io.github.xororz.localdream.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object ModelList : Screen("model_list")
    object ModelRun : Screen("model_run/{modelId}") {
        fun createRoute(modelId: String) = "model_run/$modelId"
    }

    object Upscale : Screen("upscale")

    object ImageCompare : Screen("image_compare?left={left}&right={right}") {
        fun createRoute(left: String, right: String) =
            "image_compare?left=${Uri.encode(left)}&right=${Uri.encode(right)}"
    }
}