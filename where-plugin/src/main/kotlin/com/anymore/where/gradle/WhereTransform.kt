package com.anymore.where.gradle

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import com.anymore.where.gradle.core.RegisterCodeGenerator
import com.anymore.where.gradle.core.Scanner
import java.io.File
import java.io.File.pathSeparator
import java.io.File.separator

/**
 * Created by anymore on 2021/4/23.
 */
class WhereTransform internal constructor(private val project: Project, private val logger: Logger) : Transform() {
    companion object {
        private val mScannedIndexes:MutableCollection<String> = mutableListOf()
    }

    private val mScanner = Scanner(logger, mScannedIndexes)


    override fun getName() = "EventBusRegisterTransform"

    override fun getInputTypes() = TransformManager.CONTENT_CLASS.orEmpty()

    override fun getScopes() = TransformManager.SCOPE_FULL_PROJECT.toMutableSet()

    override fun isIncremental() = false

    override fun transform(transformInvocation: TransformInvocation?) {
        val start = System.currentTimeMillis()
        mScannedIndexes.clear()
        logger.tell("transform start")
        super.transform(transformInvocation)
        val leftSlash = separator == "/"
        transformInvocation?.inputs?.forEach {
            //遍历jar
            it?.jarInputs?.forEach { jar ->
                var destName = jar.name
                if (jar.name.endsWith(".jar")) {
                    destName = jar.name.let { name -> name.subSequence(0, name.length - 4).toString() }
                }
                val hexName = DigestUtils.md2Hex(jar.file.absolutePath)
                val src = jar.file
                val dest = transformInvocation.outputProvider.getContentLocation("${destName}_$hexName", jar.contentTypes, jar.scopes, Format.JAR)
                if (mScanner.shouldProcessJar(src.absolutePath)) {
                    mScanner.scanJar(src, dest)
                }
                FileUtils.copyFile(src, dest)
            }

            it?.directoryInputs?.forEach { dir ->
                val dest = transformInvocation.outputProvider.getContentLocation(dir.name, dir.contentTypes, dir.scopes, Format.DIRECTORY)
                var root = dir.file.absolutePath
                if (!root.endsWith(pathSeparator)) {
                    root += pathSeparator
                }
                dispatchVisit(dir.file,root,leftSlash)
                FileUtils.copyDirectory(dir.file, dest)
            }
        }
        logger.tell("transform end,with[${System.currentTimeMillis() - start}ms]")
        val builderClass = mScanner.eventBusBuilderClass
        if (builderClass != null && mScannedIndexes.isNotEmpty()){
            logger.i("modify jar of:${builderClass.name}")
            RegisterCodeGenerator(logger).insert(builderClass, mScannedIndexes)
        }
    }


    private fun dispatchVisit(file: File, root: String, leftSlash: Boolean) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                dispatchVisit(it, root, leftSlash)
            }
        } else {
            var path = file.absolutePath.replace(root, "")
            if (!leftSlash) {
                path = path.replace("\\\\", "/")
            }
            if (file.isFile && mScanner.shouldProcessClass(path)) {
                mScanner.scanClass(file)
            }
        }
    }
}