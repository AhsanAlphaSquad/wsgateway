plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.2.1"
    id("io.micronaut.aot") version "4.2.1"
}

version = "0.1"
group = "example.micronaut"

repositories {
    mavenCentral()
}


val generatedDir = file("${projectDir}/src/main/java")
val codecGeneration = configurations.create("codecGeneration")

dependencies {
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micrometer:context-propagation")
    implementation("io.micronaut:micronaut-websocket")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.awaitility:awaitility:4.2.0")

    implementation("io.aeron:aeron-all:1.43.0")
    "codecGeneration"("uk.co.real-logic:sbe-tool:1.30.0")
}

application {
    mainClass.set("example.micronaut.Application")
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
    targetCompatibility = JavaVersion.toVersion("17")
}


graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("example.micronaut.*")
    }
    aot {
    // Please review carefully the optimizations enabled below
    // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading.set(false)
        convertYamlToJava.set(false)
        precomputeOperations.set(true)
        cacheEnvironment.set(true)
        optimizeClassLoading.set(true)
        deduceEnvironment.set(true)
        optimizeNetty.set(true)
    }
}

tasks.register<JavaExec>("generateCodecs") {
    group = "sbe"
    val codecsFile = "src/main/resources/sbe/protocol.xml"
    val sbeFile = "src/main/resources/sbe/sbe.xsd"
    inputs.files(codecsFile, sbeFile)
    outputs.dir(generatedDir)
    classpath = codecGeneration
    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    args = listOf(codecsFile)
    systemProperties["sbe.output.dir"] = generatedDir
    systemProperties["sbe.target.language"] = "Java"
    systemProperties["sbe.validation.xsd"] = sbeFile
    systemProperties["sbe.validation.stop.on.error"] = "true"
    outputs.dir(generatedDir)
}
