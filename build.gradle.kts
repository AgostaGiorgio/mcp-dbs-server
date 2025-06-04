plugins {
	java
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.mcp"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "1.0.0"

dependencies {
	implementation("org.springframework.boot", "spring-boot-starter-webflux")
	implementation("org.springframework.boot", "spring-boot-starter-actuator")
	implementation("org.springframework.ai", "spring-ai-starter-mcp-server-webflux")
	
	implementation("org.springframework.boot", "spring-boot-starter-data-neo4j")

	compileOnly("org.projectlombok", "lombok")
	
	annotationProcessor("org.projectlombok", "lombok")
	testImplementation("org.springframework.boot", "spring-boot-starter-test")
	testImplementation("io.projectreactor", "reactor-test")
	testRuntimeOnly("org.junit.platform", "junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.bootRun {
    args("--spring.profiles.active=local")
}