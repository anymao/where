plugins {
    `kotlin-dsl`
    id("groovy")
}

repositories {
    google()
    jcenter()
}


group = "com.anymore"

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.72")
    implementation("com.android.tools.build:gradle:4.1.3")
    implementation("commons-io:commons-io:2.8.0")
    implementation("commons-codec:commons-codec:1.15")
}


