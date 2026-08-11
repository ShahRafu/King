Title: chore(ci): pin AGP/Kotlin, update app Java/Kotlin target to 17

This PR pins the Android Gradle Plugin and Kotlin plugin versions in the root build.gradle and updates the app module to use Java/Kotlin 17 for compile/jvm target.

Changes:
- build.gradle (root): pin AGP to 8.1.2 and Kotlin plugin to 1.9.10, add ben-manes versions plugin (apply false)
- app/build.gradle: set compileOptions to JavaVersion.VERSION_17 and kotlinOptions.jvmTarget to "17"

Why:
CI was failing due to plugin/Gradle incompatibilities during script evaluation. These changes align plugin and runtime versions to reduce evaluation errors. After merge I'll inspect the Gradle debug artifacts and apply minimal fixes to any remaining incompatible plugins/dependencies.
