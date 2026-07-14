plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.21"
	id("com.github.node-gradle.node") version "7.0.1"
}

group = "com.kidz"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("io.jsonwebtoken:jjwt-api:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

node {
	version = "22.2.0"
	download = true
	nodeProjectDir = file("${project.projectDir}/admin-panel")
}

val buildAdminPanel = tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildAdminPanel") {
	dependsOn("npmInstall")
	args.set(listOf("run", "build"))
	inputs.dir("${project.projectDir}/admin-panel/src")
	inputs.file("${project.projectDir}/admin-panel/package.json")
	outputs.dir("${project.projectDir}/admin-panel/dist")
}

val copyAdminPanelToStatic = tasks.register<Copy>("copyAdminPanelToStatic") {
	dependsOn(buildAdminPanel)
	from("${project.projectDir}/admin-panel/dist")
	into("${project.layout.buildDirectory.get().asFile}/resources/main/static/admin")
}

tasks.named("processResources") {
	dependsOn(copyAdminPanelToStatic)
}
