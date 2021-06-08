plugins {
    `kotlin-dsl`
}

repositories {
    google()
    jcenter()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("com.android.tools.build:gradle:4.1.3")
    implementation("commons-io:commons-io:2.8.0")
    implementation("commons-codec:commons-codec:1.15")
}


