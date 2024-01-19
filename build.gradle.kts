import org.gradle.api.publish.maven.MavenPom
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date
import java.io.FileInputStream
import java.util.Properties
import java.util.concurrent.atomic.AtomicReference

object Versions {
    val java = JavaVersion.VERSION_17 // can't go higher than 17 because of shadowJar

    const val kotlin = "1.9.22"

    const val levelDb = "1.8" // https://mvnrepository.com/artifact/org.fusesource.leveldbjni/leveldbjni-all
    const val rocksDb = "8.9.1" // https://mvnrepository.com/artifact/org.rocksdb/rocksdbjni
    const val nettyBuffer = "4.1.105.Final" // https://mvnrepository.com/artifact/io.netty/netty-buffer

    const val junitJupiter = "5.10.1" // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter
    const val jqwik = "1.8.2" // https://mvnrepository.com/artifact/net.jqwik/jqwik
}

plugins {
    kotlin("jvm") version "1.9.22"
    id("java")
    idea
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
    `java-library`
}

project.group = "com.walkmind.extensions"
project.version = "1.8"

val publicationName = "DefaultPublication"
val artifactID = "serializers"
val licenseName = "Apache-2.0"
val licenseUrl = "http://opensource.org/licenses/apache-2.0"
val repoHttpsUrl = "https://github.com/unoexperto/extensions-serializers.git"
val repoSshUri = "git@github.com:unoexperto/extensions-serializers.git"

val awsCreds = File(System.getProperty("user.home") + "/.aws/credentials")
    .let {

        if (it.exists())
            it.readLines()
                .map {
                    val commentPos = it.indexOf('#')
                    if (commentPos >= 0) {
                        it.substring(0, commentPos).trim()
                    } else
                        it.trim()
                }
                .filter { it.isNotEmpty() }
                .fold(mutableMapOf<String, MutableMap<String, String>>() to AtomicReference<String>()) { (acc, cur), s ->

                    if (s.startsWith("[") && s.endsWith("]")) {
                        cur.set(s.substring(1, s.length - 1))
                    } else
                        if (s.contains("=")) {
                            check(cur.get() != null)
                            val items = acc.computeIfAbsent(cur.get()) { mutableMapOf<String, String>() }
                            val (k, v) = s.split("=").map { it.trim() }
                            items[k.toLowerCase()] = v
                        }

                    acc to cur
                }
                .first
        else
            emptyMap()
    }
    .get("bp")!!

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from("$buildDir/javadoc")
}

val jar = tasks["jar"] as org.gradle.jvm.tasks.Jar

fun MavenPom.addDependencies() = withXml {
    asNode().appendNode("dependencies").let { depNode ->
        configurations.implementation.get().allDependencies.forEach {
            depNode.appendNode("dependency").apply {
                appendNode("groupId", it.group)
                appendNode("artifactId", it.name)
                appendNode("version", it.version)
            }
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("s3://${awsCreds["maven_bucket"]!!}/")
            credentials(AwsCredentials::class) {

                accessKey = awsCreds["aws_access_key_id"]
                secretKey = awsCreds["aws_secret_access_key"]
            }
        }
    }

    publications {

        create(publicationName, MavenPublication::class) {
            artifactId = artifactID
            groupId = project.group.toString()
            version = project.version.toString()
            description = project.description

            artifact(jar)
            artifact(sourcesJar) {
                classifier = "sources"
            }
            artifact(javadocJar) {
                classifier = "javadoc"
            }
            pom.addDependencies()
            pom {
                packaging = "jar"
                developers {
                    developer {
                        email.set("unoexperto.support@mailnull.com")
                        id.set("unoexperto")
                        name.set("ruslan")
                    }
                }
                licenses {
                    license {
                        name.set(licenseName)
                        url.set(licenseUrl)
                        distribution.set("repo")
                    }
                }
                scm {
                    connection.set("scm:$repoSshUri")
                    developerConnection.set("scm:$repoSshUri")
                    url.set(repoHttpsUrl)
                }
            }
        }
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    implementation(kotlin("stdlib", Versions.kotlin))

    compileOnly("org.fusesource.leveldbjni:leveldbjni-all:${Versions.levelDb}")
    compileOnly("org.rocksdb:rocksdbjni:${Versions.rocksDb}")
    compileOnly("io.netty:netty-buffer:${Versions.nettyBuffer}")

    testImplementation(kotlin("test-junit5", Versions.kotlin))
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiter}")
    testImplementation("net.jqwik:jqwik:${Versions.jqwik}")
    testImplementation("org.fusesource.leveldbjni:leveldbjni-all:${Versions.levelDb}")
    testImplementation("org.rocksdb:rocksdbjni:${Versions.rocksDb}")
    testImplementation("io.netty:netty-buffer:${Versions.nettyBuffer}")
}

repositories {
    mavenCentral()
    jcenter()
    maven ("https://repo.spring.io/snapshot")
    maven ("https://repo.spring.io/release")
    flatDir {
        dirs("libs")
    }
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}

java {
    sourceCompatibility = Versions.java
    targetCompatibility = Versions.java
}

tasks {
    withType<KotlinCompile> {
        // https://github.com/JetBrains/kotlin/blob/master/compiler/util/src/org/jetbrains/kotlin/config/LanguageVersionSettings.kt
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xextended-compiler-checks",
            "-Xjsr305=strict",
            "-Xjvm-default=all",
            "-Xinline-classes",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.io.path.ExperimentalPathApi",
            "-opt-in=kotlin.js.ExperimentalJsExport",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xskip-prerelease-check",
        )
        kotlinOptions.apiVersion = "1.9"
        kotlinOptions.languageVersion = "1.9"
        kotlinOptions.jvmTarget = Versions.java.toString()
    }

    withType<Test>().all {
        testLogging.showStandardStreams = true
        testLogging.showExceptions = true
        useJUnitPlatform {
        }
    }

    withType<Wrapper>().all {
        gradleVersion = "8.5"
        distributionType = Wrapper.DistributionType.BIN
    }
}
