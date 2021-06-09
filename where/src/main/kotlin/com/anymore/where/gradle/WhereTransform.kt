package com.anymore.where.gradle

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.anymore.where.gradle.core.AppCompatActivityCodeHacker
import com.anymore.where.gradle.core.Scanner
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * Created by anymore on 2021/4/23.
 */
class WhereTransform internal constructor(
    private val project: Project,
    private val logger: Logger
) : Transform() {

    private val mScanner = Scanner(logger)

    override fun getName() = "EventBusRegisterTransform"

    override fun getInputTypes() = TransformManager.CONTENT_CLASS.orEmpty()

    override fun getScopes() = TransformManager.SCOPE_FULL_PROJECT.toMutableSet()

    override fun isIncremental() = false

    override fun transform(transformInvocation: TransformInvocation?) {
        val start = System.currentTimeMillis()
        logger.tell("transform start")
        super.transform(transformInvocation)
        transformInvocation?.inputs?.forEach {
            //遍历jar
            it?.jarInputs?.forEach { jar ->
                var destName = jar.name
                if (jar.name.endsWith(".jar")) {
                    destName =
                        jar.name.let { name -> name.subSequence(0, name.length - 4).toString() }
                }
                val hexName = DigestUtils.md2Hex(jar.file.absolutePath)
                val src = jar.file
                val dest = transformInvocation.outputProvider.getContentLocation(
                    "${destName}_$hexName",
                    jar.contentTypes,
                    jar.scopes,
                    Format.JAR
                )
                if (mScanner.shouldProcessJar(src.absolutePath)) {
                    mScanner.scanJar(src, dest)
                }
                FileUtils.copyFile(src, dest)
            }
        }
        logger.tell("transform end,with[${System.currentTimeMillis() - start}ms]")
        val appCompatActivityClass = mScanner.appCompatActivityClass
        if (appCompatActivityClass != null) {
            logger.i("modify jar of:${appCompatActivityClass.name}")
            AppCompatActivityCodeHacker(logger).insert(appCompatActivityClass)
        }
    }
}