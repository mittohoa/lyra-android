package com.mittohoa.lyra.sources

import com.mittohoa.lyra.BuildConfig

/**
 * AURA tu xung la ai khi goi ra ngoai.
 *
 * LRCLIB la kho mo do nguoi ta gop cong lai; ho de nghi moi app tu xung ten va
 * cho lien he, de khi mot app goi hong hoac gop rac thi con biet duong bao.
 * Doi lai, day cung la CHO DUY NHAT ten AURA xuat hien ben ngoai may nguoi
 * dung - moi ban loi AURA gop len deu di kem dong nay.
 *
 * KHONG KEM MOT THU GI CUA NGUOI DUNG: khong ten, khong email, khong ma may,
 * khong ma nhan dang nao. Nguoi dung gop mot ban loi thi phia ben kia biet
 * "mot ban AURA 0.3.5 nao do da gop", khong biet la ai.
 *
 * So hieu lay tu ban dung chu khong go tay. Truoc day hai noi go tay hai kieu -
 * "AURA/0.1.0" o cho tra loi va "AURA/1.0" o cho gop loi, lai con hai dia chi
 * khac nhau - trong khi app da di toi 0.3.4. Mot dong tu xung ma sai so hieu
 * thi dung phan viec cua no, la de nguoi ben kia biet ban nao gay chuyen, cung
 * hong.
 */
internal const val DANH_TINH = "AURA/${BuildConfig.VERSION_NAME} (https://mittohoa.github.io/lyra-player/)"
