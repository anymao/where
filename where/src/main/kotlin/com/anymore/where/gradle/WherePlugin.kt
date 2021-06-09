package com.anymore.where.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin

/**
 * Created by anymore on 2021/4/23.
 */
class WherePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val logger = Logger(target)
        val isApp = target.plugins.hasPlugin(AppPlugin::class)
        if (isApp) {
            val android = target.extensions.getByType(AppExtension::class)
            android.registerTransform(WhereTransform(target,logger))
        }
    }
}