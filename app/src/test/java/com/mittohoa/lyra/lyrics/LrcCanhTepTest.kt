package com.mittohoa.lyra.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Kiem phan tim tep loi nam canh tep nhac.
 *
 * Bon kieu ten deu dang ton tai ngoai doi, va bo sot mot kieu thi voi nguoi
 * dung no chi don gian la "app khong thay loi" - khong co thong bao loi nao de
 * ma lan ra.
 */
class LrcCanhTepTest {

    @get:Rule val thuMuc = TemporaryFolder()

    private fun nhac(ten: String): String =
        thuMuc.newFile(ten).absolutePath

    private fun dat(ten: String, noiDung: String = "[00:01.00]một câu") {
        File(thuMuc.root, ten).writeText(noiDung)
    }

    @Test fun `thay tep thay duoi lrc`() {
        val mp3 = nhac("bài hát.mp3")
        dat("bài hát.lrc")
        assertEquals("[00:01.00]một câu", LrcCanhTep.docCanh(mp3))
    }

    @Test fun `thay tep noi them duoi lrc`() {
        // Vai trinh tai loi dat kieu nay: "bai hat.mp3.lrc"
        val mp3 = nhac("bài hát.mp3")
        dat("bài hát.mp3.lrc")
        assertEquals("[00:01.00]một câu", LrcCanhTep.docCanh(mp3))
    }

    @Test fun `duoi viet hoa van thay`() {
        // Tep chep tu Windows sang rat hay mang duoi .LRC: ben do khong phan
        // biet hoa thuong, ben Android thi co.
        val mp3 = nhac("bài hát.mp3")
        dat("bài hát.LRC")
        assertEquals("[00:01.00]một câu", LrcCanhTep.docCanh(mp3))
    }

    @Test fun `khong co tep thi tra null chu khong nem`() {
        assertNull(LrcCanhTep.docCanh(nhac("trơ trọi.mp3")))
    }

    @Test fun `tep rong coi nhu khong co`() {
        // Mot tep .lrc rong 0 byte la rac con sot lai, khong phai loi bai hat.
        // Nhan no thi man hinh loi trong tron ma khong con di tim nguon khac.
        val mp3 = nhac("bài hát.mp3")
        dat("bài hát.lrc", "   \n\n")
        assertNull(LrcCanhTep.docCanh(mp3))
    }

    @Test fun `ten bai co nhieu dau cham van cat dung cho`() {
        // "Track 01. Nàng Thơ.mp3" - cat o dau cham CUOI CUNG moi dung.
        val mp3 = nhac("Track 01. Nàng Thơ.mp3")
        dat("Track 01. Nàng Thơ.lrc")
        assertEquals("[00:01.00]một câu", LrcCanhTep.docCanh(mp3))
    }

    @Test fun `duong dan khong co duoi van chay`() {
        val f = nhac("khôngcóđuôi")
        dat("khôngcóđuôi.lrc")
        assertEquals("[00:01.00]một câu", LrcCanhTep.docCanh(f))
    }
}
