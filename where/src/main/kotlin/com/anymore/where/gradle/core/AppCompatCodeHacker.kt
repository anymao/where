package com.anymore.where.gradle.core

import com.anymore.where.gradle.Logger
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry


/**
 * Created by anymore on 2021/4/24.
 */
internal class AppCompatCodeHacker(private val logger: Logger) {

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
                    when (jarEntry.name) {
                        ANDROIDX_FRAGMENTACTIVITY_CLASS -> {
                            logger.i("insert code to class:>>${jarEntry.name}")
                            val optedByteCode = hackActivityDispatchTouchEvent(it)
                            os.write(optedByteCode)
                        }
//                        ANDROIDX_APPCOMPATDIALOG_CLASS -> {
//                            logger.i("insert code to class:>>${jarEntry.name}")
//                            val optedByteCode = hackAppCompatDialog(it)
//                            os.write(optedByteCode)
//                        }
//                        ANDROIDX_APPCOMPATDIALOGFRAGMENT_CLASS -> {
//                            logger.i("insert code to class:>>${jarEntry.name}")
//                            val optedByteCode = hackAppCompatDialogFragment(it)
//                            os.write(optedByteCode)
//                        }
                        else -> {
                            os.write(IOUtils.toByteArray(it))
                        }
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
            val visitor = AppCompatActivityHacker(ASM6, cw)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
            cw.toByteArray()
        }
    }

    private inner class AppCompatActivityHacker(api: Int, classVisitor: ClassVisitor?) :
        ClassVisitor(api, classVisitor) {
        private var overrideDispatchTouchEvent = false
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name == HACK_METHOD) {
                logger.i("override the dispatchTouchEventMethod")
                overrideDispatchTouchEvent = true
                return AppCompatActivityInsertMethodVisitor(api, mv)
            }
            return mv
        }

        override fun visitEnd() {
            if (!overrideDispatchTouchEvent) {
                logger.i("the target class don't override dispatchTouchEventMethod,so we need insert override code")
                insertOverrideCode()
                return
            }
            super.visitEnd()
        }

        private fun insertOverrideCode() {
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
            logger.i("insertOverrideCode finished")
        }
    }

    private inner class AppCompatActivityInsertMethodVisitor(
        api: Int,
        methodVisitor: MethodVisitor?
    ) :
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
            super.visitMaxs(maxStack + 1, maxLocals)
        }
    }


    private fun hackAppCompatDialog(inputStream: InputStream): ByteArray {
        return inputStream.use {
            val cr = ClassReader(it)
            val cw = ClassWriter(cr, 0)
            val visitor = AppCompatDialogHacker(ASM6, cw)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
            cw.toByteArray()
        }
    }

    private inner class AppCompatDialogHacker(api: Int, classVisitor: ClassVisitor?) :
        ClassVisitor(api, classVisitor) {

        private var hasFiledWhereName: Boolean = false
        private var overrideDispatchTouchEvent: Boolean = false

        override fun visitField(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            if (name == "whereName") {
                hasFiledWhereName = true
            }
            return super.visitField(access, name, descriptor, signature, value)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (access == ACC_PUBLIC && name == "<init>" && descriptor == "(Landroid/content/Context;I)V") {
                logger.tell("hack public AppCompatDialog(Context context, int theme)")
                return AppCompatDialogConstructorHacker(api, mv)
            } else if (access == ACC_PROTECTED && name == "<init>" && descriptor == "(Landroid/content/Context;ZLandroid/content/DialogInterface\$OnCancelListener;)V") {
                logger.tell("hack protected AppCompatDialog(Context context, boolean cancelable,OnCancelListener cancelListener)")
                return AppCompatDialogConstructorHacker(api, mv)
            } else if (name == HACK_METHOD) {
                logger.i("override the dispatchTouchEventMethod")
                overrideDispatchTouchEvent = true
                return AppCompatDialogInsertMethodVisitor(api, mv)
            }
            return mv
        }

        override fun visitEnd() {
            if (!hasFiledWhereName) {
                val fv = cv.visitField(0, "whereName", "Ljava/lang/String;", null, null)
                fv.visitEnd()
            }
            if (!overrideDispatchTouchEvent) {
                overrideDispatchTouchEvent()
            }
            super.visitEnd()
        }

        private fun overrideDispatchTouchEvent() {
            val mv = cv.visitMethod(
                ACC_PUBLIC,
                "dispatchTouchEvent",
                "(Landroid/view/MotionEvent;)Z",
                null,
                null
            )
            mv.visitAnnotableParameterCount(1, false)
            val av = mv.visitParameterAnnotation(0, "Landroidx/annotation/NonNull;", false)
            av.visitEnd()
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "androidx/appcompat/app/AppCompatDialog",
                "getContext",
                "()Landroid/content/Context;",
                false
            )
            mv.visitVarInsn(ALOAD, 1)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitFieldInsn(
                GETFIELD,
                "androidx/appcompat/app/AppCompatDialog",
                "whereName",
                "Ljava/lang/String;"
            )
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/anymore/where/PageNavigator",
                "onTouch",
                "(Landroid/content/Context;Landroid/view/MotionEvent;Ljava/lang/String;)V",
                false
            )
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitMethodInsn(
                INVOKESPECIAL,
                "android/app/Dialog",
                "dispatchTouchEvent",
                "(Landroid/view/MotionEvent;)Z",
                false
            )
            mv.visitInsn(IRETURN)
            mv.visitMaxs(3, 2)
            mv.visitEnd()
        }
    }

    private inner class AppCompatDialogConstructorHacker(
        api: Int,
        methodVisitor: MethodVisitor?
    ) :
        MethodVisitor(api, methodVisitor) {
        override fun visitInsn(opcode: Int) {
            if (opcode == RETURN) {
                mv.visitVarInsn(ALOAD, 0)
                mv.visitVarInsn(ALOAD, 0)
                mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/Object",
                    "getClass",
                    "()Ljava/lang/Class;",
                    false
                )
                mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getCanonicalName",
                    "()Ljava/lang/String;",
                    false
                )
                mv.visitFieldInsn(
                    PUTFIELD,
                    "androidx/appcompat/app/AppCompatDialog",
                    "whereName",
                    "Ljava/lang/String;"
                )
            }
            super.visitInsn(opcode)
        }
    }

    private inner class AppCompatDialogInsertMethodVisitor(
        api: Int,
        methodVisitor: MethodVisitor?
    ) :
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
            super.visitMaxs(maxStack + 1, maxLocals)
        }
    }

    private fun hackAppCompatDialogFragment(inputStream: InputStream): ByteArray {
        return inputStream.use {
            val cr = ClassReader(it)
            val cw = ClassWriter(cr, 0)
            val visitor = AppCompatDialogFragmentHacker(ASM6, cw)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
            cw.toByteArray()
        }
    }

    private inner class AppCompatDialogFragmentHacker(api: Int, classVisitor: ClassVisitor?) :
        ClassVisitor(api, classVisitor) {

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name == "setupDialog") {
                logger.i("override the setupDialog")
                return AppCompatDialogFragmentSetupDialogMethodVisitor(api, mv)
            }
            return mv
        }

    }

    private inner class AppCompatDialogFragmentSetupDialogMethodVisitor(
        api: Int,
        methodVisitor: MethodVisitor?
    ) : MethodVisitor(api, methodVisitor) {
        private var hacked = false

        override fun visitVarInsn(opcode: Int, value: Int) {
            if (!hacked && opcode == ASTORE && value == 3) {
                super.visitVarInsn(opcode, value)
                logger.i("hack setupDialog")
                mv.visitVarInsn(ALOAD, 3)
                mv.visitVarInsn(ALOAD, 0)
                mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/Object",
                    "getClass",
                    "()Ljava/lang/Class;",
                    false
                )
                mv.visitMethodInsn(
                    INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getCanonicalName",
                    "()Ljava/lang/String;",
                    false
                )
                mv.visitFieldInsn(
                    PUTFIELD,
                    "androidx/appcompat/app/AppCompatDialog",
                    "whereName",
                    "Ljava/lang/String;"
                )
                hacked = true
            } else {
                super.visitVarInsn(opcode, value)
            }

        }
    }
}