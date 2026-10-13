import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "org.plos_clan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val bpfInput = layout.projectDirectory.file(
    "src/nativeTest/resources/bpf_base1.bin"
)
val bpfGeneratedDir = layout.buildDirectory.dir(
    "generated/bpfTest/kotlin"
)

val generateBpfFixture by tasks.registering {
    inputs.file(bpfInput)
    outputs.dir(bpfGeneratedDir)

    doLast {
        val bytes = bpfInput.asFile.readBytes()
        val output = bpfGeneratedDir.get()
            .file("fixtures/BpfFixture.kt")
            .asFile

        output.parentFile.mkdirs()

        val values = bytes.asList()
            .chunked(32)
            .joinToString(",\n") { row ->
                "    " + row.joinToString(", ") { it.toString() }
            }

        output.writeText(
            """
            |package fixtures
            |
            |internal fun bpfProgramBytes(): ByteArray = byteArrayOf(
            |$values
            |)
            |
            """.trimMargin()
        )
    }
}


kotlin {
    linuxArm64()
    linuxX64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            executable {
                entryPoint = "main"
            }
        }

        compilations.named("test") {
            compileTaskProvider.configure {
                dependsOn(generateBpfFixture)
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(libs.kotlinxSerializationJson)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        nativeTest {
            kotlin.srcDir(generateBpfFixture)
        }
    }
}
