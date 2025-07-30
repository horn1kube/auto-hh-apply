plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    java
}

group = "app"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // HTML parsing
    implementation("org.jsoup:jsoup:1.17.2")
    
    // Environment configuration
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    
    // Database
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic")
    
    // Selenium for web scraping
    implementation("org.seleniumhq.selenium:selenium-java:4.15.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.15.0")
    
    // HTTP client (for Ollama API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Ollama Java client
    implementation("io.github.ollama4j:ollama4j:1.0.100")
    
    // PDF parsing
    implementation("org.apache.pdfbox:pdfbox:2.0.29")

    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Task для создания JAR с зависимостями
tasks.register<Jar>("fatJar") {
    dependsOn.addAll(listOf("compileJava", "compileTestJava", "processTestResources"))
    archiveClassifier.set("standalone")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to "app.Application")) }
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
        sourcesMain.output
    from(contents)
} 