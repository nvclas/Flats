plugins {
    alias(libs.plugins.runPaper) apply false
    alias(libs.plugins.paperweight.userdev) apply false
    alias(libs.plugins.flyway) apply false
    alias(libs.plugins.lombok) apply false
    alias(libs.plugins.shadow) apply false
}

group = "de.nvclas"
version = "2.1.3"
