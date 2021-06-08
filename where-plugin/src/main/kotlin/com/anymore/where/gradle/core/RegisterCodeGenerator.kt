package com.anymore.where.gradle.core

import org.apache.commons.io.IOUtils
import com.anymore.where.gradle.Logger
import org.objectweb.asm.*
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by anymore on 2021/4/24.
 */
internal class RegisterCodeGenerator(private val logger: Logger) {

    fun insert(jarFile: File, scannedIndexClasses: MutableCollection<String>): File {
        return insertRegisterCodeToBuilder(jarFile, scannedIndexClasses)
    }

    private fun insertRegisterCodeToBuilder(originJar: File, scannedIndexClasses: MutableCollection<String>): File {
        if (scannedIndexClasses.isEmpty()) {
            return originJar
        }
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
                    if (jarEntry.name == EVENTBUS_BUILDER_NAME) {
                        logger.i("insert code to class:>>${jarEntry.name}")
                        val optedByteCode = hackEventBusBuilderConstructor(it, scannedIndexClasses)
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

    private fun hackEventBusBuilderConstructor(inputStream: InputStream, scannedIndexClasses: MutableCollection<String>): ByteArray {
        return inputStream.use {
            val cr = ClassReader(it)
            val cw = ClassWriter(cr, 0)
            val visitor = InsertClassVisitor(Opcodes.ASM6, cw, scannedIndexClasses)
            cr.accept(visitor, ClassReader.EXPAND_FRAMES)
            cw.toByteArray()
        }
    }

    private inner class InsertClassVisitor(api: Int, classVisitor: ClassVisitor?, private val scannedIndexClasses: MutableCollection<String>) : ClassVisitor(api, classVisitor) {
        override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name == CLASS_STATIC_BLOCK) {
                mv = InsertMethodVisitor(Opcodes.ASM6, mv, scannedIndexClasses)
            }
            return mv
        }
    }

    private inner class InsertMethodVisitor(api: Int, methodVisitor: MethodVisitor?, val scannedIndexClasses: MutableCollection<String>) : MethodVisitor(api, methodVisitor) {
        override fun visitInsn(opcode: Int) {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                scannedIndexClasses.forEach {
                    mv.visitTypeInsn(Opcodes.NEW, it)
                    mv.visitInsn(Opcodes.DUP)
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, it, "<init>", "()V", false)
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "add", "(L$EVENTBUS_INDEX_INTERFACE_NAME;)V", false)
                }
            }
            super.visitInsn(opcode)
        }


        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}