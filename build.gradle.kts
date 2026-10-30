import dev.sebi.sebchievements.build.GenerateAdvancements

plugins {
    java
}

group = "dev.sebi"
version = "0.4.0"

val paperApi = "io.papermc.paper:paper-api:26.3.build.223-stable"

// Datapack format of the target server (data_major in Paper's version.json); Paper 26.3 = 121.
val packDataMajor = 121
val packDataMinor = 0

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly(paperApi)

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val generated = layout.buildDirectory.dir("generated")

val generateAdvancements = tasks.register<GenerateAdvancements>("generateAdvancements") {
    group = "build"
    description = "Validates the advancement catalog and emits the datapack and AdvancementIds"
    catalog = layout.projectDirectory.file("src/main/catalog/advancements.yml")
    namespace = "smp"
    dataMajor = packDataMajor
    dataMinor = packDataMinor
    resourcesDir = generated.map { it.dir("resources") }
    javaDir = generated.map { it.dir("java") }
}

sourceSets.main {
    java.srcDir(generateAdvancements.flatMap { it.javaDir })
    resources.srcDir(generateAdvancements.flatMap { it.resourcesDir })
}

tasks.jar {
    archiveFileName = "sebchievements.jar"
}

tasks.test {
    useJUnitPlatform()
}

// ./gradlew deploy copies the jar into the local test server (stop/start it with scripts/dev-server.sh)
tasks.register<Copy>("deploy") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(System.getProperty("user.home") + "/mc-server/plugins")
}
