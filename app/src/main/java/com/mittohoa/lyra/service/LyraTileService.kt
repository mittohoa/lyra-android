package com.mittohoa.lyra.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mittohoa.lyra.R

/**
 * O Quick Settings: vuot thanh thong bao xuong, cham mot cai la bat/tat loi noi.
 *
 * Day la thu thay cho phim tat toan cuc cua ban Windows, va hop hon han:
 *   - Khong app nao chiem mat duoc. Ban Windows dat Ctrl+Alt+L, con
 *     Ctrl+Alt+mui-ten thi da bi driver man hinh Intel giu san.
 *   - Voi toi duoc ngay ca khi dang o trong app khac - dung luc can nhat,
 *     vi ca app nay sinh ra la de dung TRONG LUC nghe nhac o app khac.
 *
 * `TileService` chi song trong luc o duoc hien; moi trang thai nam o `Lyra`.
 */
class LyraTileService : TileService() {

    override fun onStartListening() {
        refresh()
    }

    override fun onClick() {
        // Khong can mo app: khung noi gan thang vao WindowManager nen bat duoc
        // tu day, mien la nguoi dung da cap quyen ve de len app khac
        Lyra.toggleOverlay(applicationContext)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = Lyra.overlay.isShowing

        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        // Chu phu chi co tu Android 10; bo qua o ban cu hon
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (on) R.string.tile_on else R.string.tile_off)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.updateTile()
    }
}
