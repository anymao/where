plugins {
    `kotlin-dsl`
}

repositories {
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.31")
    implementation("com.android.tools.build:gradle:4.1.3")
    implementation("commons-io:commons-io:2.8.0")
    implementation("commons-codec:commons-codec:1.15")
}


