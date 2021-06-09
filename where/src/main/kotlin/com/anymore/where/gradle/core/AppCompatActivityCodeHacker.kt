package com.anymore.where.gradle.core

import org.apache.commons.io.IOUtils
import com.anymore.where.gradle.Logger
import org.objectweb.asm.*
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by anymore on 2021/4/24.
 */
internal class AppCompatActivityCodeHacker(private val logger: Logger) {

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
            val visitor = InsertClassVisitor(Opcodes.ASM6, cw)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
            cw.toByteArray()
        }
    }

    private inner class InsertClassVisitor(api: Int, classVisitor: ClassVisitor?) : ClassVisitor(api, classVisitor) {
        override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            var mv = super.visitMethod(access, name, descriptor, signature, exceptions)

            return mv
        }
    }

    private inner class InsertMethodVisitor(api: Int, methodVisitor: MethodVisitor?) : MethodVisitor(api, methodVisitor) {
        override fun visitInsn(opcode: Int) {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {

            }
            super.visitInsn(opcode)
        }


        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}