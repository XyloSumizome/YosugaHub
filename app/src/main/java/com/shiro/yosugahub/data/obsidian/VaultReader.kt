package com.shiro.yosugahub.data.obsidian

/**
 * Vault からの**読み取り**の抽象化(設計書v5 §13)。
 *
 * 既存の [KnowledgeStore] が書き込み側、こちらが読み取り側。
 * 初期実装は SAF([SafVaultReader])だが、Dropbox API 等へ差し替えられるようにしておく
 * (§12: Dropbox が SAF で選べるかは実機確認待ちのため、ここを疎結合にしておく)。
 */
interface VaultReader {

    /** Vault 内の `.md` を再帰的に列挙する。本文は読まない。 */
    suspend fun listNotes(): VaultListing

    /** 1 件のノート本文を読む。読めなければ null。 */
    suspend fun readNote(documentUri: String): String?

    /** 表示・出力に使う Vault 名(選択フォルダの名前)。未設定なら空文字。 */
    suspend fun vaultName(): String
}
