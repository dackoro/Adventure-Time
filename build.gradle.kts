plugins {
    java
}

group = "dev.dackoro"
version = "0.0.6"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven {
        name = "hytale"
        url = uri("https://maven.hytale.com/release")
    }
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:+")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("Adventure_time_DEV")
    from(".") {
        include("manifest.json")
        include("Common/**")
        include("Server/**")
        include("icon-256.png")
    }
}
