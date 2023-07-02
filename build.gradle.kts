// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.0-rc01" apply false
    id("com.android.library") version "8.1.0-rc01" apply false
    kotlin("android") version "1.8.22" apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
