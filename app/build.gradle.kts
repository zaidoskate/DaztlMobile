plugins {
    alias(libs.plugins.android.application)
    id("com.google.protobuf") version "0.9.4"
}

android {
    namespace = "com.example.daztlmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.daztlmobile"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main").java.srcDirs(
            "src/main/java",
            "build/generated/source/proto/main/java",
            "build/generated/source/proto/main/grpc"
        )
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.64.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout.v214)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    annotationProcessor(libs.compiler)
    implementation(libs.glide)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.glide)
    annotationProcessor (libs.compiler)


    // gRPC para Android (lite)
    implementation(libs.grpc.okhttp.v1640)
    implementation(libs.grpc.protobuf.lite.v1640)
    implementation(libs.grpc.stub.v1640)
    implementation (libs.grpc.stub.v1572)
    implementation(libs.javax.annotation.api)
    implementation(libs.protobuf.javalite)
    implementation (libs.material.v1110)
    implementation (libs.viewpager2)
    implementation(libs.transition)
    implementation ("com.google.android.material:material:1.12.0")


}

configurations.all {
    resolutionStrategy {
        force("androidx.transition:transition:1.5.0")
    }
}

