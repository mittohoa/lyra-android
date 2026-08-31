package com.mittohoa.lyra.data

import android.content.Context
import kotlin.math.roundToInt

/**
 * Co chu goi y cho khung loi noi, suy ra tu be ngang man hinh.
 *
 * Mot con so chot cung khong the vua cho moi may. Khung loi noi la thu de LIEC
 * MAT DOC trong luc dang lam viec khac - dang xem YouTube, dang lai xe, dang
 * nau an - nen chu nho qua thi no vo dung. Ma nguoi dung thuong khong nghi toi
 * viec vao chinh; ho chi thay tinh nang nay do roi tat di.
 *
 * Vi sao khong de `sp` lo het: `sp` chi lo phan MAT DO va co chu he thong, tuc
 * la 26sp tren may nao cung to bang nhau tinh theo xen-ti-met. Nhung mot cai
 * dien thoai gap mo ra rong gap doi thi cung mot co chu ay lai hoa nho so voi
 * khung - vi khung chiem tron be ngang. Cai ta can la co chu theo TI LE VOI
 * KHUNG, va khung thi bang be ngang man hinh.
 *
 * Moc neo: may rong 411dp (Pixel 6 Pro) -> 32sp. Cac co khac noi suy tuyen tinh
 * roi kep vao khoang con doc duoc.
 */

/** Chu cao bang ngan nay phan be ngang man hinh. */
private const val FONT_RATIO = 0.078f

private const val FONT_MIN = 20f
private const val FONT_MAX = 44f

/**
 * Co chu hop voi mot man hinh rong `smallestWidthDp`.
 *
 * Dung BE NGANG NHO NHAT chu khong phai be ngang hien tai: xoay ngang man hinh
 * khong lam nguoi dung ngoi xa hon, nen chu khong co ly do gi phai to len - ma
 * doi co chu moi lan xoay may thi chi lam giat mat.
 */
fun suggestFontSizeSp(smallestWidthDp: Int): Float =
    (smallestWidthDp * FONT_RATIO).coerceIn(FONT_MIN, FONT_MAX).roundToInt().toFloat()

fun suggestFontSizeSp(context: Context): Float =
    suggestFontSizeSp(context.resources.configuration.smallestScreenWidthDp)
