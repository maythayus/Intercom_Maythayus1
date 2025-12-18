// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
    }
}

plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false
}

// Configure all projects
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }
    
    // Add KAPT arguments to all projects that have the kapt plugin
    plugins.withId("org.jetbrains.kotlin.kapt") {
        configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
            useBuildCache = true
            correctErrorTypes = true
            javacOptions {
                option("-Xmaxerrs", 500)
                // Add these exports for Java 17 compatibility
                option("--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
                option("--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
                option("--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
                option("--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
                option("--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
                option("--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}