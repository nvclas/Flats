import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    java
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
    alias(libs.plugins.lombok)
    alias(libs.plugins.runPaper)
}

group = "de.nvclas"
version = "1.0.0"

dependencies {
    implementation(project(":flats-core"))
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.annotations)

    compileOnly(libs.flyway.core)
    compileOnly(libs.caffeine)
}

paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
        options.encoding = "UTF-8"
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "sqliteVersion" to libs.versions.sqlite.get(),
        "flywayVersion" to libs.versions.flyway.get(),
        "caffeineVersion" to libs.versions.caffeine.get()
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveBaseName.set("Flats")
    archiveClassifier.set("")
}

tasks.assemble {
    dependsOn(tasks.reobfJar)
}
