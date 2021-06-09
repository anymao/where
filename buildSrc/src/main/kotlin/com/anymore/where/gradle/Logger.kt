package com.anymore.where.gradle

import org.gradle.api.Project

/**
 * Created by anymore on 2021/4/23.
 */
internal class Logger(project: Project) {
    private val sLogger = project.logger
    private val tag = "Where:>>${project.name}"

    fun t(message: String){
        sLogger.trace("[$tag]:$message")
    }

    fun i(message: String) {
        sLogger.info("[$tag]:$message")
    }

    fun d(message: String) {
        sLogger.debug("[$tag]:$message")
    }

    fun e(message: String,throwable: Throwable? = null) {
        sLogger.error("[$tag]:$message",throwable)
    }

    fun tell(message: String){
        println("[$tag]:$message")
    }
}