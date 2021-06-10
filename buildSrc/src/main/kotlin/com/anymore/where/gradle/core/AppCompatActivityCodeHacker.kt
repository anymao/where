package com.anymore.where.gradle.core

import com.anymore.where.gradle.Logger
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry


/**
 * Created by anymore on 2021/4/24.
 */
internal class AppCompatActivityCodeHacker(private val logger: Logger) {
    private var overrideDispatchTouchEvent = false

    fun insert(jarFile: File): File {
        return insertRegisterCodeToBuilder(jarFile)
    }

    private fun insertRegisterCodeToBuilder(originJar: File): File {
        val optJar = File(originJar.parent, originJar.name + ".opt")
        if (optJar.exists()) {
            optJar.delete()
        }
        JarOutputStream(optJar.outputStream()).use { os ->
            val jarFile = JarFile(originJar)
            jarFile.entries().iterator().forEach { jarEntry ->
                val zip = ZipEntry(jarEntry.name)
                jarFile.getInputStream(zip).use {
                    os.putNextEntry(zip)
                    if (jarEntry.name == ANDROIDX_APPCOMPATACTIVITY_CLASS) {
                        logger.i("insert code to class:>>${jarEntry.name}")
                        val optedByteCode = hackActivityDispatchTouchEvent(it)
                        os.write(optedByteCode)
                    } else {
                        os.write(IOUtils.toByteArray(it))
                    }
                }
                os.closeEntry()
            }
            jarFile.close()
        }
        if (originJar.exists()) {
            originJar.delete()
        }
        optJar.renameTo(originJar)
        return originJar
    }

    private fun hackActivityDispatchTouchEvent(inputStream: InputStream): ByteArray {
        return inputStream.use {
            val cr = ClassReader(it)
            val cw = ClassWriter(cr, 0)
            val visitor = InsertClassVisitor(ASM6, cw)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
            cw.toByteArray()
        }
    }

    private inner class InsertClassVisitor(api: Int, classVisitor: ClassVisitor?) :
        ClassVisitor(api, classVisitor) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name == HACK_METHOD) {
                logger.tell("override the dispatchTouchEventMethod")
                overrideDispatchTouchEvent = true
                return InsertMethodVisitor(api, mv)
            }
            return mv
        }

        override fun visitEnd() {
            if (!overrideDispatchTouchEvent) {
                logger.tell("the target class don't override dispatchTouchEventMethod,so we need insert override code")
                insertOverrideCode()
                return
            }
            super.visitEnd()
        }

        private fun insertOverrideCode() {
            logger.tell(cv.javaClass.name)
            val mv = cv.visitMethod(
                ACC_PUBLIC,
                "dispatchTouchEvent",
                "(Landroid/view/MotionEvent;)Z",
                null,
                null
            )
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/anymore/where/PageNavigator",
                "onActivityTouchEvent",
                "(Landroidx/appcompat/app/AppCompatActivity;Landroid/view/MotionEvent;)V",
                false
            )
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitMethodInsn(
                INVOKESPECIAL,
                "android/app/Activity",
                "dispatchTouchEvent",
                "(Landroid/view/MotionEvent;)Z",
                false
            )
            mv.visitInsn(IRETURN)
            mv.visitMaxs(2, 2)
            mv.visitEnd()
            cv.visitEnd()
            logger.tell("insertOverrideCode finished")
        }
    }

    private inner class InsertMethodVisitor(api: Int, methodVisitor: MethodVisitor?) :
        MethodVisitor(api, methodVisitor) {

        override fun visitCode() {
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/anymore/where/PageNavigator",
                "onActivityTouchEvent",
                "(Landroidx/appcompat/app/AppCompatActivity;Landroid/view/MotionEvent;)V",
                false
            )
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}