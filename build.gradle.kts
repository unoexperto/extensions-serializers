import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Versions {
    val java = JavaVersion.VERSION_17 // can't go higher than 17 because of shadowJar

    const val kotlin = "1.9.23"

    const val nettyBuffer = "4.1.107.Final" // https://mvnrepository.com/artifact/io.netty/netty-buffer
}

plugins {
    kotlin("jvm") version "1.9.23"
    id("java")
    idea
    id("com.vanniktech.maven.publish") version "0.28.0"
    `java-library`
}

project.group = "com.walkmind.extensions"
project.version = "1.9"
project.description = "Collections of highly customizable binary serializers for Netty's ByteBuf class."

val artifactID = "serializers"
val licenseName = "Apache-2.0"
val licenseUrl = "http://opensource.org/licenses/apache-2.0"
val repoHttpsUrl = "https://github.com/unoexperto/extensions-serializers.git"
val repoSshUri = "git@github.com:unoexperto/extensions-serializers.git"

mavenPublishing {

    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        )
    )

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(project.group.toString(), artifactID, project.version.toString())

    pom {
        name.set(project.name)
        description.set(project.description)
        inceptionYear.set("2020")
        url.set(repoHttpsUrl)
        licenses {
            license {
                name.set(licenseName)
                url.set(licenseUrl)
                distribution.set("repo")
            }
        }
        developers {
            developer {
                email.set("unoexperto.support@mailnull.com")
                id.set("unoexperto")
                name.set("ruslan")
                url.set("https://github.com/unoexperto/")
            }
        }
        scm {
            connection.set("scm:$repoSshUri")
            developerConnection.set("scm:$repoSshUri")
            url.set(repoHttpsUrl)
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

    compileOnly("io.netty:netty-buffer:${Versions.nettyBuffer}")

    testImplementation(kotlin("test-junit5", Versions.kotlin))
    testImplementation("io.netty:netty-buffer:${Versions.nettyBuffer}")
}

repositories {
    mavenCentral()
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
