@file:Suppress("UnstableApiUsage")
@file:SuppressLint("JcenterRepositoryObsolete")

import android.annotation.SuppressLint
import pers.zhc.plugins.BuildUtils.*
import pers.zhc.plugins.FileUtils.requireCreate
import pers.zhc.plugins.NdkVersion
import pers.zhc.plugins.RegexUtils
import pers.zhc.plugins.SdkPath
import pers.zhc.tools.plugin.ndk.AndroidAbi
import pers.zhc.tools.plugin.ndk.cpp.CppBuildPlugin
import pers.zhc.tools.plugin.ndk.cpp.CppBuildPlugin.CppBuildPluginExtension
import pers.zhc.tools.plugin.ndk.rust.RustBuildPlugin
import pers.zhc.tools.plugin.ndk.rust.RustBuildPlugin.RustBuildPluginExtension
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap
import pers.zhc.plugins.`BuildUtils2$`.`MODULE$` as BuildUtils2
import pers.zhc.tools.plugin.util.`FileUtils$`.`MODULE$` as FileUtils

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}
apply<RustBuildPlugin>()
apply<CppBuildPlugin>()

val rootProjectDir = rootProject.projectDir

val base64Encoder = Base64.getEncoder()!!
val commitLogResult = try {
    val gitVersion = BuildUtils2.checkGitVersion()!!
    println("Found git $gitVersion")

    try {
        val log = BuildUtils2.getGitCommitLog(projectDir)!!
        println("git log string size: ${log.toByteArray().size}")
        log.ifEmpty { "Unknown" }
    } catch (e: Exception) {
        throw GradleException("Execution failed", e)
    }
} catch (e: Exception) {
    if (e is GradleException) {
        throw e
    }

    println("Found git error: $e; disable git commit log injection")
    "Unknown"
}

val configPropertiesFile = File(rootDir, "config.properties")
requireCreate(configPropertiesFile)

val ndkFormatHintMsg = """Format: <ABI-name>-<native-API-version>
Example:
ndk.target=arm64-v8a-29,x86_64-29"""

val properties = openPropertiesFile(configPropertiesFile)!!
val propNdkTarget = properties.getProperty("ndk.target") ?: run {
    throw GradleException("Please define \"ndk.target\" in $configPropertiesFile.path\n$ndkFormatHintMsg")
}

val ndkTargets = propNdkTarget.split(',').map { it.trim() }.map {
    if (!it.matches(Regex("^.*-[0-9]+\$"))) {
        throw GradleException("Wrong NDK target format: $it\n$ndkFormatHintMsg")
    }
    val captured = RegexUtils.capture(it, "^(.*)-([0-9]+)\$")
    mapOf(
        Pair("abi", TargetAbi.from(captured[0][1])!!), Pair("api", captured[0][2].toInt())
    )
}
val ndkTargetsForConfigs = ndkTargets.map {
    mapOf(
        Pair("abi", it["abi"].toString()), Pair("api", it["api"])
    )
}

val foundSdkDir = try {
    file(SdkPath.getSdkPath(project))
} catch (_: FileNotFoundException) {
    throw GradleException("Cannot found sdk path. Please define \"sdk.dir\" in the project \"local.properties\"")
}
val detectedNdkVersion = NdkVersion.getLatestNdkVersion(foundSdkDir) ?: run {
    NdkVersion.readLocalProperties(project) ?: run {
        throw GradleException(
            "Cannot get NDK version. Please check NDK directory \"\$SDK/ndk/\" " + "or try to define \"ndk.version\" in the project \"local.properties\""
        )
    }
}

android {
    namespace = "pers.zhc.tools"
    signingConfigs {
        val configs = asMap
        configs["debug"]!!.apply {
            storeFile = file("test.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }
    compileSdk = 33
    defaultConfig {
        applicationId = "pers.zhc.tools"
        minSdk = 21
        targetSdk = 33

        val verInfo = gVersion()!! as ArrayList<*>
        versionCode = verInfo[0] as Int
        versionName = verInfo[1].toString()

        ndk {
            abiFilters.addAll(ndkTargets.map {
                it["abi"].toString()
            })
        }

        val compressedGitLog = BuildUtils2.lzmaCompress(commitLogResult.toByteArray(StandardCharsets.UTF_8))

        buildConfigField(
            "String[]", "commitLogEncodedSplit", BuildUtils2.longStringToStringArray(
                base64Encoder.encodeToString(compressedGitLog),
                100
            )
        )
    }
    buildTypes {
        val types = asMap
        types["debug"]!!.apply {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules-debug.pro")
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs["debug"]
        }
        types["release"]!!.apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            isDebuggable = true
            isJniDebuggable = true
            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
//    lint {
//        isCheckReleaseBuilds = false
//        isAbortOnError = false
//    }

    ndkVersion = detectedNdkVersion

    sourceSets {
        val sets = asMap
        sets["main"]!!.apply {
            jniLibs.srcDirs("jniLibs")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}


val appProject = project
val jniOutputDir = File(appProject.projectDir, "jniLibs").also { it.mkdirs() }

val opensslDir = getOpensslDir(configPropertiesFile)!!

val rustBuildExtraEnv = HashMap<String, String>()
ndkTargets.forEach {
    val env = (getRustOpensslBuildEnv(AndroidAbi.from(it["abi"].toString()).toRustTarget()) as Map<*, *>).map { e ->
        Pair(e.key.toString(), e.value.toString())
    }.toMap()
    val opensslPath = getOpensslPath(opensslDir, it["abi"] as TargetAbi)
    rustBuildExtraEnv[env["libDir"].toString()] = opensslPath.lib!!.path
    rustBuildExtraEnv[env["includeDir"].toString()] = opensslPath.include!!.path
}

val rustBuildTargetEnv = HashMap<String, Map<String, String>>()
ndkTargets.forEach {
    val abi = it["abi"].toString()
    rustBuildTargetEnv[abi] = HashMap<String, String>().apply {
        this["SQLITE3_INCLUDE_DIR"] =
            "$projectDir/app/src/main/cpp/third_party/jni-lib/third_party/my-cpp-lib/third_party/sqlite3-single-c"
        this["SQLITE3_LIB_DIR"] = File(jniOutputDir, abi).path
    }
}

configure<RustBuildPluginExtension> {
    ndkDir.set(android.ndkDirectory.path)
    targets.set(ndkTargetsForConfigs)
    buildType.set("release")
    srcDir.set(File(appProject.projectDir, "src/main/rust").path)
    outputDir.set(jniOutputDir.path)
    extraEnv.set(rustBuildExtraEnv)
    targetEnv.set(rustBuildTargetEnv)
}


val copyOpensslLibsTask = project.task("copyOpensslLibs") {
    doLast {
        ndkTargets.forEach {
            val abi = it["abi"] as TargetAbi
            val opensslPath = getOpensslPath(opensslDir, abi)
            listOf(
                "libssl.so", "libcrypto.so"
            ).map { libName ->
                File(opensslPath.lib!!, libName)
            }.forEach { file ->
                if (!file.exists()) {
                    throw GradleException("Required OpenSSL library file not found: ${file.path}")
                }

                val outputDir = File(jniOutputDir, abi.toString()).also { d -> d.mkdirs() }
                FileUtils.copyFile(file, File(outputDir, file.name))
            }
        }
    }
}


val compileRustTask: Task = appProject.tasks.getByName(RustBuildPlugin.TASK_NAME())
compileRustTask.dependsOn(copyOpensslLibsTask)

val sdkDir = android.sdkDirectory
val ndkDir = android.ndkDirectory
val tools = Tools(ndkDir, sdkDir)
val cmakeVersion = tools.cMakeVersion as String

println(
    """Build environment info:
    |SDK path: ${android.sdkDirectory.path}
    |NDK path: ${android.ndkDirectory.path}
    |NDK version: $detectedNdkVersion
    |CMake version: $cmakeVersion
    |NDK targets: $ndkTargets
    |Rust build extra env: $rustBuildExtraEnv
    |Rust build target env: $rustBuildTargetEnv
""".trimMargin()
)


repositories {
    mavenCentral()
    google()
    @Suppress("DEPRECATION")
    jcenter {
        content {
            // https://github.com/reddit/IndicatorFastScroll/issues/45
            includeGroup("com.reddit")
        }
    }
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:5.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
    implementation("com.google.android.material:material:1.8.0-alpha03")
    implementation("com.github.mik3y:usb-serial-for-android:3.3.0")
    implementation("com.github.bczhc:java-lib:18a858c167")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("me.zhanghai.android.fastscroll:library:1.1.8")
    implementation("com.github.bczhc:jni-java:9205bf19ea")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.20")
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.reddit:indicator-fast-scroll:1.4.0")
    implementation("com.quiph.ui:recyclerviewfastscroller:1.0.0")
}

task("saveNdkPath") {
    doLast {
        val path = android.ndkDirectory.path
        File(rootDir, "tmp").writeText(path)
    }
}

val cleanAllTask = rootProject.task("cleanAll") {
    dependsOn("clean", ":app:cleanRust", ":app:cleanCpp")
}
cleanAllTask.doLast {
    listOf(
        buildDir,
        rootProject.buildDir,
        jniOutputDir,
        File(rootProjectDir, ".gradle"),
        File(rootProjectDir, "app/.cxx"),
        File(rootProjectDir, "buildSrc/build"),
        File(rootProjectDir, "buildSrc/.gradle")
    ).forEach { requireDelete(it) }
}

fun requireDelete(file: File) {
    if (file.exists()) {
        if (!delete(file)) {
            throw IOException("Failed to delete ${file.path}")
        }
    }
}

val cmakeDefsMap = HashMap<String, Map<String, String>>()
ndkTargets.forEach {
    val abi = it["abi"] as TargetAbi
    val opensslPath = getOpensslPath(opensslDir, abi)
    cmakeDefsMap[abi.toString()] = mapOf(
        Pair("OPENSSL_INCLUDE_DIR", opensslPath.include.path),
        Pair("OPENSSL_LIBS_DIR", opensslPath.lib.path)
    )
}

configure<CppBuildPluginExtension> {
    srcDir.set("$projectDir/src/main/cpp")
    ndkDir.set(android.ndkDirectory.path)
    targets.set(ndkTargetsForConfigs)
    buildType.set("release")
    outputDir.set(jniOutputDir.path)
    cmakeBinDir.set(tools.cmakeBinDir.path)
    cmakeDefs.set(cmakeDefsMap)
}

val compileCppTask: Task = project.tasks.getByName("compileCpp")

val compileJniTask = task("compileJni") {
    dependsOn(compileCppTask)
    dependsOn(compileRustTask)
}

appProject.tasks.getByName("preBuild").dependsOn(compileJniTask)
