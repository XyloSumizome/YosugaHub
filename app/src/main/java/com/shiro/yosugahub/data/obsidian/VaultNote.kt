package com.shiro.yosugahub.data.obsidian

/**
 * Vault 内の Markdown ノート 1 件のメタ情報(設計書v5 §13)。
 * 本文は持たない。一覧表示のために軽量に保ち、本文は選択されたものだけ後から読む。
 */
data class VaultNote(
    /** Vault ルートからの相対パス(例: `Games/ANRI/Design/Lighting.md`)。 */
    val relativePath: String,
    /** ファイル名(例: `Lighting.md`)。 */
    val name: String,
    /** 本文を読むための document URI 文字列。 */
    val documentUri: String,
    /** 最終更新時刻(エポックミリ秒)。取得できない場合は 0。 */
    val lastModified: Long,
    /** バイトサイズ。取得できない場合は 0。 */
    val size: Long,
) {
    /** 所属フォルダ(Vault 直下なら空文字)。 */
    val folder: String get() = relativePath.substringBeforeLast('/', "")

    /** 拡張子を除いた表示用のタイトル。 */
    val title: String get() = name.removeSuffix(EXTENSION)

    companion object {
        const val EXTENSION = ".md"
    }
}

/** Vault 一覧取得の結果。未設定と失敗を呼び出し側が区別できるようにする。 */
sealed interface VaultListing {
    data class Success(val notes: List<VaultNote>) : VaultListing

    /** Vault フォルダが未選択(設定画面へ誘導する)。 */
    data object NotConfigured : VaultListing

    /** 権限失効・IO エラーなど。 */
    data class Failed(val reason: String) : VaultListing
}
