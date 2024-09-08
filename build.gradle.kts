plugins {
    java
    alias(libs.plugins.runPaper)
    alias(libs.plugins.paperweight.userdev)
}

group = "de.nvclas"
version = "0.36"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(libs.annotations)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    paperweight.paperDevBundle(libs.versions.paper)

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
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

tasks {
    runServer {
        minecraftVersion("1.20.6")
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.assemble {
    dependsOn(tasks.reobfJar)
}