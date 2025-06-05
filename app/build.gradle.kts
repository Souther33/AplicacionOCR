plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.aplicacionocr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aplicacionocr"
        minSdk = 30
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
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable = true
    }
}

dependencies {

    // To recognize Latin script
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // To recognize Chinese script
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // To recognize Devanagari script
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    // To recognize Japanese script
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    // To recognize Korean script
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")

    //implementation("com.google.firebase:firebase-ml-vision:24.1.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("com.squareup.picasso:picasso:2.8")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation ("jp.wasabeef:glide-transformations:4.3.0")

    //rich text editor
    implementation("jp.wasabeef:richeditor-android:2.0.0")

    // OpenCV
    //implementation("org.opencv:opencv-android:4.5.5")
    //implementation(project(":openCVLibrary"))

    // Paso de datos entre fragments
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Generar PDF más preciso
    implementation("com.itextpdf:itext7-core:7.2.3")
    // Para HTML a PDF
    implementation("com.itextpdf:html2pdf:4.0.4")

    //PDF Viewer
    implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")
    //implementation("com.github.barteksc:android-pdf-viewer:2.8.2")



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}