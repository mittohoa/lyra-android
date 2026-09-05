import com.android.build.api.artifact.SingleArtifact
import java.io.FileInputStream
import java.util.Properties

/**
 * So phien ban, khai o day chu khong nhet thang vao `defaultConfig`.
 *
 * Ten tep ban gop cung phai mang so nay - xem khoi `androidComponents` o duoi.
 * Khai mot cho thi doi phien ban la sua mot dong; khai hai cho thi som muon co
 * mot ban gop mang ten cua ban truoc, va do la loai nham lan khong ai phat hien
 * cho toi luc nop nham file len Play.
 */
val maPhienBan = 29
val tenPhienBan = "0.3.18"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

/**
 * Cau hinh ky, doc tu mot file NGOAI kho ma nguon.
 *
 * Khong co file thi ban phat hanh van dung duoc, chi la khong duoc ky - dung
 * de nguoi khac clone ve van build duoc. Doi lai ho phai tu tao khoa cua minh,
 * va do la dung: khoa ky la danh tinh cua nguoi phat hanh, khong phai cua ma
 * nguon.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

android {
    namespace = "com.mittohoa.lyra"
    compileSdk = 36

    defaultConfig {
        // Cung mot id voi ban Windows. Khac `namespace` o tren - `namespace`
        // la goi ma nguon, con day la danh tinh cua app tren may va tren Play.
        // Hai thu nay khong bat buoc phai trung nhau.
        applicationId = "com.mittohoa.lyra_player"
        // 26 la moc toi thieu that su: TYPE_APPLICATION_OVERLAY chi co tu day.
        // Cac kieu cua so overlay cu hon deu da bi Android chan.
        minSdk = 26
        // Google Play đòi targetSdk không được cũ hơn một năm so với bản Android
        // mới nhất. 36 là Android 16.
        targetSdk = 36
        versionCode = maPhienBan
        versionName = tenPhienBan

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * Hai ban phan phoi, cung mot ma nguon.
     *
     * `sideload` - ban day du, cai bang tay. Co tai nhac.
     * `play`     - ban len Google Play. KHONG mang ma tai nao ca.
     *
     * Tach bang bo ma nguon (`src/sideload/`, `src/play/`) chu khong bang mot
     * co bat/tat luc chay. Mot cai co van de lai toan bo ma trong file cai dat,
     * va nguoi duyet Play mo file ra xem thi thay - khac biet giua "khong dung"
     * va "khong co" la khac biet that.
     *
     * Cung `applicationId`: day la MOT app, hai duong phat hanh. Nguoi dung
     * khong cai duoc ca hai mot luc, va dung ra la vay.
     */
    flavorDimensions += "phanphoi"
    productFlavors {
        create("sideload") {
            dimension = "phanphoi"
        }
        create("play") {
            dimension = "phanphoi"
        }
    }

    signingConfigs {
        create("phathanh") {
            val path = keystoreProperties.getProperty("storeFile")
            if (path != null && file(path).exists()) {
                storeFile = file(path)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("phathanh")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Play canh bao "ban gop chua ma goc va ban chua tai bieu tuong go
            // loi len". Dong nay la cau tra loi dung cho canh bao do - nhung
            // HOM NAY NO KHONG SINH RA GI, va da do de biet chac.
            //
            // Lyra khong viet dong ma mach may nao. Bon thu vien .so trong ban
            // nop deu la nhi phan dung san cua ML Kit va AndroidX, va ca bon
            // deu da lot sach: doc bang section cua ELF khong thay .symtab,
            // khong thay .debug_*. Khong co ky hieu thi khong co gi de dong
            // goi, va Google khong phat hanh ky hieu go loi cho cac thu vien
            // do. Canh bao cua Play vi vay khong go duoc tu phia minh.
            //
            // Van giu dong nay: ngay nao Lyra tu viet ma mach may, hoac mot
            // thu vien nao do ship ban chua lot, ky hieu se duoc dong goi ma
            // khong ai phai nho ra viec nay nua.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            // Bo dong khong dung khi chay thu. Bo may dich cua ML Kit nang
            // 15,6 MB CHO MOI KIEN TRUC CPU, nen mot ban go loi gom du bon
            // kien truc len toi 76 MB - moi lan cai lai la mot phut ngoi doi.
            // Ban phat hanh khong bi anh huong: xem `splits` ben duoi.
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
    }

    /**
     * Moi kien truc CPU mot goi rieng - CHI cho ban cai tay.
     *
     * Bo may dich cua ML Kit nang 15,6 MB cho moi kien truc, nen mot goi gom du
     * bon kien truc bat nguoi dung tai ba phan tu khong bao gio chay duoc tren
     * may ho.
     *
     * Ban gop (.aab) cho Play thi TU lam viec tach nay, va Play giao dung goi
     * may nguoi dung can. Lam ca hai la mau thuan - tu AGP 8.13 no bao loi thang
     * "Please disable building multiple APKs when building an Android app
     * bundle". Nen tat tach goi khi lenh dang chay la dung ban gop.
     *
     * `isUniversalApk` de mo cho ban tai thang tu ngoai Play: van can mot goi
     * chay duoc o moi may, chi la no nang.
     */
    val dungBanGop = gradle.startParameter.taskNames.any { it.contains("undle") }

    splits {
        abi {
            isEnable = !dungBanGop
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            // `android.util.Log` khong ton tai trong ban chay tren may tinh, va
            // mac dinh moi loi goi toi no NEM ngay. Ma mot ham dang chay tot
            // tren dien thoai thi khong nen truot bai kiem chi vi no co ghi mot
            // dong nhat ky. Bat co nay thi cac ham do tra ve gia tri rong.
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        compose = true
        // De doc duoc so phien ban luc chay, phuc vu kiem ban moi
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

/**
 * Chep ban gop ra mot ban sao MANG SO PHIEN BAN trong ten.
 *
 * VI SAO CAN. Gradle luon xuat ra dung mot ten - `app-play-release.aab` - va
 * ghi de len tep cu moi lan dung. Dung lien may ban trong mot buoi thi cac ban
 * truoc bien mat khong dau vet, va den luc muon doi chieu "ban nop tuan truoc
 * co mang thay doi nay chua" thi khong con gi de mo ra xem.
 *
 * CHEP chu khong DOI TEN: ban goc de nguyen cho cu, nen moi thu quen duong dan
 * do - Play Console, kich ban dung san, hay chinh thoi quen cua nguoi dung -
 * deu khong hong. Ban danh so la mot ban luu nam rieng.
 *
 * DAT SANG THU MUC KHAC, khong ghi chung vao `outputs/bundle/`. Thu muc do do
 * AGP quan ly va co tac vu khac cua no doc vao; ghi them tep la vao giua thi
 * Gradle bao loi phu thuoc ngam va ca lan dung that bai. Da vap dung cho nay.
 *
 * Lay tep qua `SingleArtifact.BUNDLE` chu khong tu doan duong dan: dia chi thu
 * ra nam trong tay AGP va da doi vai lan qua cac doi. Hoi AGP thi doi bao nhieu
 * lan nua cung khong phai sua o day.
 *
 * `finalizedBy` de chay TU DONG sau moi lan dung ban gop. Bat nguoi ta nho goi
 * them mot lenh nghia la se co lan quen, ma lan quen do trung dung lan can nho
 * nhat.
 */
androidComponents {
    onVariants { bienThe ->
        val ten = bienThe.name.replaceFirstChar { it.uppercase() }
        val chep = tasks.register<Copy>("chepBanGopDanhSo$ten") {
            from(bienThe.artifacts.get(SingleArtifact.BUNDLE))
            into(layout.buildDirectory.dir("outputs/ban-gop-danh-so/${bienThe.name}"))
            rename { "lyra-${bienThe.name}-$tenPhienBan.aab" }
        }
        tasks.matching { it.name == "bundle$ten" }.configureEach { finalizedBy(chep) }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
