package com.anymore.where.gradle.core

import com.anymore.where.gradle.Logger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

/**
 * Created by anymore on 2021/4/24.
 */
internal class Scanner(private val logger: Logger, private val mScannedIndexClasses: MutableCollection<String>) {

    var eventBusBuilderClass: File? = null
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
                if (entry.isDirectory || !name.endsWith(".class")){
                    return@forEach
                }
                logger.i(name)
                if (EVENTBUS_BUILDER_NAME == name) {
                    eventBusBuilderClass = dest
                } else {
                    it.getInputStream(entry).use { stream ->
                        scanClass(stream)
                    }
                }
            }
        }
    }

    fun scanClass(src: File) {
        scanClass(src.inputStream())
    }

    private fun scanClass(input: InputStream) {
        input.use {
            val cr = ClassReader(it)
            val cw = ClassWriter(cr, 0)
            val visitor = EventBusInfoIndexClassVisitor(Opcodes.ASM6, cw)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
        }
    }

    inner class EventBusInfoIndexClassVisitor(api: Int, classVisitor: ClassVisitor?) : ClassVisitor(api, classVisitor) {
        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            super.visit(version, access, name, signature, superName, interfaces)
            if (name != null && interfaces?.contains(EVENTBUS_INDEX_INTERFACE_NAME) == true) {
                mScannedIndexClasses.add(name)
                logger.i("find class impl:$name")
            }
        }
    }
}