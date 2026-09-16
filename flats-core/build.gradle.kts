plugins {
    java
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.lombok)
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.annotations)

    compileOnly(libs.flyway.core)
    compileOnly(libs.caffeine)

    testImplementation(libs.flyway.core)
    testImplementation(libs.caffeine)
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
