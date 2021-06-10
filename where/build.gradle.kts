plugins {
    `kotlin-dsl`
//    `maven`
    id("groovy")
    id("com.github.dcendents.android-maven")
}

repositories {
    google()
    jcenter()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    implementation("com.android.tools.build:gradle:4.1.3")
    implementation("commons-io:commons-io:2.8.0")
    implementation("commons-codec:commons-codec:1.15")
}

//val archivesBaseName = "where"
//val groupId = "com.github.anymao"
//val versionName = "1.0.0"
//tasks {
//    "uploadArchives"(Upload::class) {
//        repositories {
//            withConvention(MavenRepositoryHandlerConvention::class) {
//                mavenDeployer {
//                    withGroovyBuilder {
//                        "repository"("url" to mavenLocal().url)
//                    }
//                    pom.project {
//                        withGroovyBuilder {
//                            "groupId"(groupId)
//                            "artifactId"(archivesBaseName)
//                            "version"(versionName)
//                        }
//                        name = "where"
//                        description = "Help find target activity,fragment,dialog name "
//                    }
//                }
//            }
//        }
//    }
//}
