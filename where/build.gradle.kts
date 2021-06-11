plugins {
    `kotlin-dsl`
    id("android-maven")
    id("groovy")
    id("com.github.dcendents.android-maven")
}

repositories {
    google()
    jcenter()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    implementation("com.android.tools.build:gradle:4.1.3")
    implementation("commons-io:commons-io:2.8.0")
    implementation("commons-codec:commons-codec:1.15")
}

//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            groupId = "com.github.anymao"
//            artifactId = "where"
//            version = "1.0.0"
//            from(components["java"])
//        }
//    }
//}