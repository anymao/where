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
            target.extensions.create<WhereExtension>("where")
            val appAssembleRelease = ":${target.name}:assembleRelease"
            val isDebug = !target.gradle.startParameter.taskNames.any { it == appAssembleRelease }
            target.dependencies {
                add("debugImplementation", "com.github.anymao.where:where-runtime:1.0.4")
            }
            val transform = WhereTransform(target, logger)
            android.registerTransform(transform)
            target.afterEvaluate {
                val enable = target.extensions.getByName<WhereExtension>("where").enable
                logger.i("where enable:$enable")
                if (enable && isDebug) {
                    logger.tell("${target.name} debuggable is true,register transform!")
                    transform.enabled = true
                } else {
                    transform.enabled = false
                    logger.tell("the where plugin is disabled or buildType is release,skip registerTransform")
                }
            }
        }
    }
}