package com.shiro.yosugahub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * ブルータリスト形状(v5 UI)。**角丸を全廃**する。
 * カード・ダイアログ・テキスト欄・チップが直角になる(ボタンは別途ラッパーで直角化)。
 */
val TerminalShapes = Shapes(
    extraSmall = RoundedCornerShape(0),
    small = RoundedCornerShape(0),
    medium = RoundedCornerShape(0),
    large = RoundedCornerShape(0),
    extraLarge = RoundedCornerShape(0),
)
