package com.mittohoa.lyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiem dinh dang tep sao luu.
 *
 * Day la cho duy nhat trong app ma mot loi nho an bang mat: xuat ra thieu hay
 * nhap vao lech mot dong thi nguoi dung chi phat hien luc da can toi ban sao
 * luu - tuc luc ban goc khong con nua. Nen kiem ky hon binh thuong, va kiem
 * dung nhung thu de vo: dong trong, ky tu tab, chinh chuoi "===" nam trong loi.
 */
class SaoLuuLoiTest {

    private fun ban(caSi: String, ten: String, loi: String) =
        ManualLyricStore.BanLoi("a".repeat(40), caSi, ten, loi)

    @Test fun `xuat roi nhap lai duoc dung noi dung`() {
        val goc = listOf(
            ban("Noo Phước Thịnh", "Những Kẻ Mộng Mơ", "[00:12.30]Từng tia sáng\n[00:15.00]mong manh"),
            ban("Sơn Tùng M-TP", "Chúng Ta Của Hiện Tại", "lời chưa căn giờ\ndòng hai")
        )
        val lai = SaoLuuLoi.nhap(SaoLuuLoi.xuat(goc))

        assertEquals(2, lai.size)
        assertEquals(goc[0].loi, lai[0].loi)
        assertEquals(goc[1].loi, lai[1].loi)
        assertEquals("Noo Phước Thịnh", lai[0].caSi)
        assertEquals("Chúng Ta Của Hiện Tại", lai[1].tenBai)
    }

    @Test fun `dong trong giua bai duoc giu nguyen`() {
        // Nguoi ta de mot dong trong giua cac doan. Mat dong do thi loi van doc
        // duoc, nhung moc gio lech het tu do tro di neu la ban da can gio.
        val loi = "[00:01.00]đoạn một\n\n[00:09.00]đoạn hai"
        val lai = SaoLuuLoi.nhap(SaoLuuLoi.xuat(listOf(ban("A", "B", loi))))
        assertEquals(loi, lai.single().loi)
    }

    @Test fun `cau hat bat dau bang ba dau bang khong lam vo tep`() {
        // Dem dong chu khong dung dau phan cach chinh la de chiu duoc cho nay.
        val loi = "[00:01.00]bình thường\n=== không phải dấu phân cách\n[00:05.00]hết"
        val lai = SaoLuuLoi.nhap(SaoLuuLoi.xuat(listOf(ban("A", "B", loi))))
        assertEquals(1, lai.size)
        assertEquals(loi, lai.single().loi)
    }

    @Test fun `ten mang ky tu tab khong lam lech cot`() {
        val lai = SaoLuuLoi.nhap(SaoLuuLoi.xuat(listOf(ban("Ca\tSĩ", "Tên\tBài", "[00:01.00]x"))))
        assertEquals("Ca Sĩ", lai.single().caSi)
        assertEquals("Tên Bài", lai.single().tenBai)
        assertEquals("[00:01.00]x", lai.single().loi)
    }

    @Test fun `ban ghi khong ten van xuat va nhap duoc`() {
        // Ban ghi luu tu truoc khi co bang ten. Khoa la thu duy nhat con lai.
        val lai = SaoLuuLoi.nhap(SaoLuuLoi.xuat(listOf(ban("", "", "[00:01.00]x"))))
        assertEquals(1, lai.size)
        assertEquals("a".repeat(40), lai.single().khoa)
    }

    @Test fun `tep la tra ve rong chu khong nem loi`() {
        assertTrue(SaoLuuLoi.nhap("").isEmpty())
        assertTrue(SaoLuuLoi.nhap("một tệp văn bản bất kỳ\ndòng hai").isEmpty())
        assertTrue(SaoLuuLoi.nhap("{\"json\": true}").isEmpty())
    }

    @Test fun `mot khoi hong khong lam mat cac khoi con lai`() {
        val tot = SaoLuuLoi.xuat(listOf(ban("A", "Bài Một", "[00:01.00]x")))
        val hong = "===\tkhông-phải-số\tkhoa\tB\tBài Hỏng\n[00:01.00]y\n"
        val tot2 = SaoLuuLoi.xuat(listOf(ban("C", "Bài Ba", "[00:02.00]z")))
            .lines().drop(1).joinToString("\n")

        val lai = SaoLuuLoi.nhap(tot + hong + tot2)
        val ten = lai.map { it.tenBai }
        assertTrue("phải giữ được bài trước khối hỏng", ten.contains("Bài Một"))
        assertTrue("phải giữ được bài sau khối hỏng", ten.contains("Bài Ba"))
    }

    @Test fun `so dong to hon ca tep khong nuot phan con lai`() {
        val gian = "LYRA-LOI\t1\n===\t9999\tkhoa\tA\tBài Dối\n[00:01.00]x\n"
        assertTrue(SaoLuuLoi.nhap(gian).isEmpty())
    }

    @Test fun `tep rong van co dau nhan de nhan ra`() {
        val chu = SaoLuuLoi.xuat(emptyList())
        assertTrue(chu.startsWith(SaoLuuLoi.NHAN))
        assertTrue(SaoLuuLoi.nhap(chu).isEmpty())
    }
}
