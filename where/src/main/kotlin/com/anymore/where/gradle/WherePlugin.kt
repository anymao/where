package com.anymore.where.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/**
 * Created by anymore on 2021/4/23.
 */
class WherePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val logger = Logger(target)
        val isApp = target.plugins.hasPlugin(AppPlugin::class)
        if (isApp) {
            val android = target.extensions.getByType(AppExtension::class)
            val enable = true
            if (enable) {
                target.dependencies {
                    add("debugImplementation", "com.github.anymao:where-runtime:master-SNAPSHOT")
                }
                android.registerTransform(WhereTransform(target, logger))
            } else {
                logger.tell("the where plugin is disabled,skip registerTransform")
            }
        }
    }
}