# SpectatorLock
[![Paper 1.21.11](https://img.shields.io/badge/Paper-1.21.11-brightgreen.svg)](https://fill-ui.papermc.io/projects/paper)
[![GitHub release](https://img.shields.io/github/release/gorogoro-space/SpectatorLock.svg)](https://github.com/gorogoro-space/SpectatorLock/releases)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/gorogoro-space/SpectatorLock/issues)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://github.com/gorogoro-space/SpectatorLock/blob/main/LICENSE)

This plugin prevents players without permission from switching to spectator mode.

権限を持たないプレイヤーが観戦モード(スペクテイター)に切り替えるのを止めるプラグインです。

# Requirements
- Paper 1.21.11 以降(1.21.11 未満のサーバーでは読み込まれません)
- Java 21

# Installation method
Please place the .jar file in the Paper plugins folder.

.jar ファイルを Paper の plugins フォルダに置き、サーバーを再起動してください。

# Features
- `spectatorlock.bypass` の権限を持たないプレイヤーが観戦モードに切り替えようとすると、取り消してメッセージを表示します
  - `/gamemode`、ゲームモード切り替え画面(F3+F4・F3+N)、ほかのプラグインからの切り替えのどれでも止めます
  - `/gamemode` で他人を観戦モードにしようとしたときは、コマンドを実行した人にメッセージを表示します
  - ハードコアで死んだときに観戦モードになるのは止めません
  - 既に観戦モードのプレイヤーは、そのままにします(切り替えの瞬間だけを止めます)
- 観戦モードを止めるワールドを限定できます(既定では全ワールドで止めます)
- 観戦モードの人が、観戦メニュー(数字キー)でほかのプレイヤーの所へテレポートするのを止められます(既定では無効)
- 設定はサーバーを再起動せずに読み込み直せます

# Commands
- `/spectatorlock reload` — config.yml を読み込み直します。`spectatorlock.reload` の権限が必要です(コンソールからも実行できます)

# Permissions
すべて既定では OP が持っています。

- `spectatorlock.bypass` — 観戦モードに切り替えられます
- `spectatorlock.teleport` — `block-spectator-teleport` が有効でも、観戦メニューでテレポートできます
- `spectatorlock.reload` — `/spectatorlock reload` を使えます

# Config
`plugins/SpectatorLock/config.yml`
- `restrict-worlds.enabled` — `true` にすると、`restrict-worlds.worlds` に書いたワールドでだけ観戦モードを止めます。`false` なら全ワールドで止めます。既定は `false`
- `restrict-worlds.worlds` — 観戦モードを止めるワールドの名前の一覧。既定は `world`・`world_nether`・`world_the_end`
- `block-spectator-teleport` — `true` にすると、観戦メニューでほかのプレイヤーの所へテレポートするのを止めます。`restrict-worlds.enabled` が `true` なら、行き先が一覧のワールドのときだけ止めます。`/tp` などのコマンドは止めません。既定は `false`
- `language` — メッセージの言語。`en`(英語)か `ja`(日本語)。既定は `en`
- `messages.en.*` / `messages.ja.*` — 各言語のメッセージ。文は自由に書き換えられます。`{player}` はプレイヤー名に置き換わります
  - ほかの言語を使うときは、`messages` の下に `fr:` などを書き足し、`language: fr` にします。書いていない項目は英語で表示します
  - `language` の言語が `messages` に無いときは、起動時に警告を出して英語にします

config.yml が既にある場合、足りない項目は起動時に既定値で追記されます。

# Disclaimer
Do not assume any responsibility by use. Please use it at your own risk.

## ビルド

JDK 21 が必要です。Gradle はラッパー(`gradlew`)が自動でダウンロードするので、別途インストールする必要はありません。

```
gradlew.bat clean build
```

(macOS / Linux では `./gradlew clean build`)

## IntelliJ IDEA でのビルド手順

本プロジェクトはビルドツールに Gradle を使用しています。
IntelliJ IDEA 上で正しくプラグイン（JARファイル）を生成するには、以下の手順を実行してください。
「ビルド → アーティファクトのビルド」はクラスファイルが入らないことがあるので使わないでください。

### 🛠️ ビルド手順

1. IntelliJ IDEA の画面右端にある **「Gradle」タブ** をクリックして開きます。
2. プロジェクト名（SpectatorLock）を展開し、 **`Tasks`** ツリーを開きます。
3. リスト内にある **`clean`** をダブルクリックして実行します（古いビルドキャッシュを削除します）。
4. 続けてリスト内にある **`build`** をダブルクリックして実行します。

### 📦 生成されたファイルの場所
ビルドが成功すると、プロジェクトのルート直下に `build/libs` フォルダが作成（または更新）され、その中に中身の詰まった正しい JAR ファイルが生成されます。

* **生成先:** `build/libs/SpectatorLock-<version>.jar`(例: `SpectatorLock-1.0.jar`。バージョンは `gradle.properties` の `version`)

この JAR ファイルを Minecraft サーバーの `plugins` フォルダに配置してください。

## 開発(IntelliJ IDEA で Claude Code を使う)

このリポジトリには、AI コーディングツール [Claude Code](https://docs.claude.com/ja/docs/claude-code/overview) 向けの作業ルールを書いた `CLAUDE.md` があります。IntelliJ IDEA で使う手順は次のとおりです。

1. Claude の有料プラン(Pro / Max)か、Anthropic API のアカウントを用意します。
2. Windows では、先に [Git for Windows](https://git-scm.com/downloads/win) をインストールします。
3. Claude Code 本体をインストールします。Windows では PowerShell で次を実行します。
   ```
   irm https://claude.ai/install.ps1 | iex
   ```
4. IntelliJ IDEA の「設定 → プラグイン → Marketplace」で「Claude Code」を検索してインストールし、IDE を再起動します。検索結果には Anthropic 以外が作った似た名前のプラグインも表示されます。プラグイン名の下に書かれた提供元が「Anthropic」になっているものを選んでください。
5. プロジェクトを開いて、ターミナルで `claude` を実行します(`Ctrl+Esc` でも起動できます)。初回はブラウザでログインします。

起動すると `CLAUDE.md` が自動で読み込まれ、このプロジェクトの設計方針に沿って作業します。

## ライセンス

[LICENSE](LICENSE) を参照してください。

## kubotan へのメモ

リポジトリを新規作成したら、**必ず Watch を設定すること**(忘れない!)。
GitHub の自動 Watch 機能は 2025 年 5 月に廃止されたため、設定しないと他の人が立てた issue や PR の通知が届かない。

1. リポジトリのページ右上の **「Watch」** を押す
2. **「Custom」** を選び、**Issues** と **Pull requests** にチェックを入れる
