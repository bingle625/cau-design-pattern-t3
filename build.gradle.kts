plugins {
    application
}

group = "com.holub"
version = "1.02"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Compile for Java 17 bytecode with whatever JDK (17+) runs Gradle,
// so team members don't all need the same JDK installed.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

application {
    mainClass = "com.holub.life.Life"
}

// Make `java -jar build/libs/holub-life-<version>.jar` work, like the original HolubLife.jar.
tasks.jar {
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
}

tasks.test {
    useJUnitPlatform()
    // Model tests must not need a display.
    systemProperty("java.awt.headless", "true")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
