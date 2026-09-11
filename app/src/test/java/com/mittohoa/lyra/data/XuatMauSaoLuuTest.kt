package com.mittohoa.lyra.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Sinh ra một tệp sao lưu MẪU, để bên Windows đọc thử bằng chính bộ đọc của nó.
 *
 * VÌ SAO CẦN MỘT TỆP THẬT. Hai bộ đọc viết bằng hai ngôn ngữ, nằm trong hai
 * kho mã, và không có gì bắt chúng phải hiểu định dạng giống nhau. Mỗi bên tự
 * kiểm lấy bên mình thì cả hai đều xanh trong khi vẫn không đọc được của nhau —
 * và chỗ ấy chỉ lộ ra ở tay người dùng, lúc họ chép tệp sang máy tính.
 *
 * Tệp sinh ra ở đây được chép sang `media-player/scripts/mau-sao-luu.txt` và
 * bộ kiểm bên ấy đọc chính nó. Một tệp, hai bộ đọc, cùng một câu trả lời.
 *
 * SINH RA CHỨ KHÔNG GÕ TAY. Gõ tay một tệp mẫu nghĩa là kiểm bộ đọc Windows
 * với thứ TÔI NGHĨ Android ghi ra, chứ không phải thứ nó ghi ra thật.
 */
class XuatMauSaoLuuTest {

    @Test fun `sinh tep mau cho ben Windows doc`() {
        val loi = SaoLuuLoi.xuat(
            listOf(
                ManualLyricStore.BanLoi(
                    khoa = "lyra://may/1000005468",
                    caSi = "Sơn Tùng M-TP",
                    tenBai = "Chúng Ta Của Hiện Tại",
                    loi = "[00:12.30]câu thứ nhất\n=== dòng này KHÔNG phải đầu khối\n[00:20.00]câu cuối"
                ),
                ManualLyricStore.BanLoi(
                    khoa = "lyra://may/1000005469",
                    caSi = "Mỹ Tâm",
                    tenBai = "Đúng Cũng Thành Sai",
                    loi = "[00:01.00]một dòng duy nhất"
                )
            )
        )

        val nghe = SaoLuuLichSu.xuat(
            listOf(
                LanNghe(
                    diaChi = "lyra://may/1000005468",
                    ten = "Chúng Ta Của Hiện Tại",
                    caSi = "Sơn Tùng M-TP",
                    luc = 1789057997795L,
                    soLan = 3
                ),
                LanNghe(
                    diaChi = "",
                    ten = "Bài ở app khác",
                    caSi = "Ai Đó",
                    app = "com.zing.mp3",
                    luc = 1789057783732L
                )
            )
        )

        val raw = SaoLuuTatCa.xuat(
            loi = loi,
            nghe = nghe,
            thich = listOf("lyra://may/1000005468"),
            canBang = SaoLuuTatCa.CanBang(bat = true, mau = 2)
        )

        // Ghi vào thư mục dựng chứ không vào mã nguồn: đây là thứ sinh ra được,
        // và một tệp sinh ra mà nằm trong mã nguồn thì sớm muộn ai đó sẽ sửa
        // tay nó.
        val ra = File("build/mau-sao-luu.txt")
        ra.parentFile?.mkdirs()
        ra.writeText(raw)

        // Đọc ngược lại bằng chính bộ đọc bên này, để cái tệp đem đi kiểm chéo
        // chắc chắn là tệp HỢP LỆ chứ không phải một tệp hỏng mà cả hai bên
        // cùng đọc hỏng giống nhau.
        val doc = SaoLuuTatCa.tach(raw)
        assertTrue(SaoLuuLoi.nhap(doc[SaoLuuTatCa.PHAN_LOI].orEmpty()).size == 2)
        assertTrue(SaoLuuLichSu.nhap(doc[SaoLuuTatCa.PHAN_NGHE].orEmpty()).size == 2)
        assertTrue(SaoLuuTatCa.docThich(doc[SaoLuuTatCa.PHAN_THICH].orEmpty()).size == 1)
        assertTrue(SaoLuuTatCa.docCanBang(doc[SaoLuuTatCa.PHAN_CANBANG].orEmpty()) != null)
    }
}
