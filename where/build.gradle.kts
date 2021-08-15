plugins {
    `kotlin-dsl`
//    id("android-maven")
    id("groovy")
//    id("com.github.dcendents.android-maven")
    `maven-publish`
}

repositories {
    google()
    jcenter()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.70")
    implementation("com.android.tools.build:gradle:3.5.0")
    implementation("commons-io:commons-io:2.8.0")
    implementation("commons-codec:commons-codec:1.15")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.anymao.where"
            artifactId = "where"
            version = "1.0.0"
            from(components["kotlin"])
        }
    }

    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url = uri("E:\\mvn")
        }
    }
}