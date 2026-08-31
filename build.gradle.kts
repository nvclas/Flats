import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    java
    alias(libs.plugins.runPaper)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.flyway)
    alias(libs.plugins.lombok)
}

group = "de.nvclas"
version = "2.1.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.annotations)
    implementation(libs.flyway.core)
    implementation(libs.caffeine)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.paper)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.sqlite)
    testRuntimeOnly(libs.junit.platform.launcher)
}

paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
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

tasks.test {
    useJUnitPlatform()
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

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
