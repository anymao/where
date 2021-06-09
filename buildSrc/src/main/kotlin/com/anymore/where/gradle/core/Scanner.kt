package com.anymore.where.gradle.core

import com.anymore.where.gradle.Logger
import java.io.File
import java.util.jar.JarFile

/**
 * Created by anymore on 2021/4/24.
 */
internal class Scanner(private val logger: Logger) {

    var appCompatActivityClass: File? = null
        private set

    fun shouldProcessJar(path: String): Boolean {
        return true
    }

    fun shouldProcessClass(className: String): Boolean {
        return true
    }

    fun scanJar(src: File, dest: File) {
        JarFile(src).use {
            it.entries().iterator().forEach { entry ->
                val name = entry.name
                if (entry.isDirectory || !name.endsWith(".class")) {
                    return@forEach
                }
                logger.i(name)
                if (ANDROIDX_APPCOMPATACTIVITY_CLASS == name) {
                    logger.tell("find class:$name")
                    appCompatActivityClass = dest
                }
            }
        }
    }
}