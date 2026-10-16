# SpectatorLock

権限の無いプレイヤーが観戦モード(スペクテイター)に切り替えるのを止める Paper 用プラグイン(Paper 1.21.11 以降・Java 21 必須)。
リポジトリ: https://github.com/gorogoro-space/SpectatorLock (GPL-3.0)

## 作業の進め方(必ず守ること)

- **設計が確定するまで実装しない。** 機能追加や仕様変更は、まず設計案(何を・なぜ・どう変えるか、影響範囲)を提示し、承認を得てからコードを書く。
- 判断が必要な点は、選択肢を示して質問する。勝手に決めない。
- やり取りは日本語で行う。新しく書くコードのコメントは日本語でよい。既存の英語のコメントは、頼まれない限り書き換えない。
- 変更は必要最小限にする。頼まれていないリファクタリングや機能追加はしない。
- 作業後は、変更・追加・削除したファイルの一覧と変更内容を報告する。
- 仕様を変えたら README.md と CLAUDE.md も合わせて更新する。
- `git push` の前には必ず確認を取る。コミットは意味のある単位で分ける。
- リポジトリへの初回の `git push` の前には、「GitHub で Watch 設定(Custom → Issues と Pull requests)はしましたか?」と日本語で確認する。自動 Watch は廃止されており、設定しないと issue や PR の通知が届かない。
- コミットメッセージや PR に `Co-Authored-By: Claude` などの署名を付けない。`.claude/` と `CLAUDE.local.md` は Git に入れない(`.git/info/exclude` で除外済み)。
- サーバーや開発 PC の環境情報(プラグインの構成・バージョン、設定ファイルの内容、JDK の場所など)は内部情報なので、Git に入るファイル(README.md・CLAUDE.md・コード・コミットメッセージ・issue・PR)に書かない。`CLAUDE.local.md` にだけ書く。
- 実装中に設計の抜けや穴に気づいたら、黙って対処せず報告して相談する。
- プルリクエストをチェックするときは、次の観点で確認して報告する。
  - 変更概要(何を・なぜ変えているか)
  - 脆弱性(権限チェックの漏れ、抜け道になる操作、入力値の検証など)
  - 安全面(再読み込み時の後始末、他プラグインの妨げなど)
  - 性能(TPS など)の低下(高頻度イベントでの重い処理、メインスレッドでの同期 I/O、定期タスクの追加など。「最重要の設計方針」に沿っているか)

## 環境

- Paper 1.21.11(`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`、`compileOnly`)/ `plugin.yml` の `api-version: '1.21.11'`(1.21.11 未満のサーバーでは読み込まれない)。勝手に変えない
- **Java 21 必須。** `build.gradle.kts` の toolchain で JDK 21 を使い、Java 21 の class ファイルを出力する。勝手に変えない
  - paper-api 26.x は「JVM 25 以上専用」と宣言しているため、Java 21 で出力する設定ではビルドできない
- ビルド: Gradle(Kotlin DSL、wrapper 9.7.1)。`gradlew.bat clean build`(Windows)。**JDK 21 で実行する**(JDK の場所は `CLAUDE.local.md`)。「ビルドして」と言われたら常にクリーンビルドする。成果物は `build/libs/SpectatorLock-<version>.jar`
  - IntelliJ の「アーティファクトのビルド」はクラスファイルが入らないことがある。必ず Gradle でビルドする
- バージョンは `gradle.properties` の `version` だけで管理する。`plugin.yml` の `version: '${version}'` は `processResources` でビルド時に置き換わる
- パッケージ: `space.gorogoro.spectatorlock`(**すべて小文字**。大文字が混ざると plugin.yml の main と一致せず起動しない)
- 動作確認はサーバーを再起動して行う。PlugManX での読み込みは権限やコマンドの登録が不完全になることがある。jar を差し替えるときは先に古い jar を消す(同じ名前のプラグインが 2 つあると、どちらが読まれるかわからない)
- サーバーと開発 PC の環境情報は `CLAUDE.local.md`(Git に入れない)を参照する

## 最重要の設計方針

### TPS に影響させない
- 高頻度イベント(PlayerInteractEvent、PlayerMoveEvent、AsyncPlayerChatEvent など)を使うときは、安い判定を先に行い、対象外なら即座に抜ける
- BlockPhysicsEvent、VehicleMoveEvent など発生頻度が極端に高いイベントは使わない
- メインスレッドでファイルや DB の同期 I/O をしない(起動時・リロード時に一度だけ行う小さなファイルの読み込みは除く)
- 設定ファイルを追加する場合は、起動時・リロード時に一度だけ解析して保持する
- 定期タスクは最小限にし、追加するときは頻度と理由を設計案に書く

### データの保存
- プラグインフォルダ以外には何も書き込まない

### 他プラグインとの関係
- 既存の機能を妨げないこと。イベントをキャンセルする範囲は必要最小限にする

## 機能仕様

(現在の実装の動作。仕様を変えたらここを更新する)

- **起動時(`onEnable`)**: `config.yml` が無ければ jar 内のものを保存する。足りない項目があれば `copyDefaults(true)` と `saveConfig()` で既定値を追記する。設定を読み込んで保持する。定期タスクは登録しない
- **設定**: `restrict-worlds.enabled`(既定 false)・`restrict-worlds.worlds`(既定 `world`・`world_nether`・`world_the_end`)・`block-spectator-teleport`(既定 false)・`language`(既定 `en`)
  - ワールド限定が無効なら全ワールドで制限する。有効なら `worlds` に書いたワールド(名前の完全一致)でだけ制限する
- **メッセージ**: `language` で選んだ `messages.<language>.*` を読み込んで保持する(jar には `en`・`ja` を入れてある。利用者が `messages.fr` などを書き足せばその言語も使える。その言語の項目が無ければ警告を出して `en` にする。足りない項目は jar 内の `en` で補う)。表示用の文字列はコードに直書きしない。`{player}` を置き換える。`plugin.yml` のコマンドと権限の説明は切り替えられないので英語
- **観戦モードへの切り替え(`PlayerGameModeChangeEvent`、LOW、取り消し済みは無視)**: 新しいゲームモードが観戦でなければ即座に抜ける。原因が `HARDCORE_DEATH`(ハードコアで死んだとき)なら止めない。今いるワールドが制限の対象で、`spectatorlock.bypass` を持たない人なら取り消す
  - 原因が `COMMAND`(`/gamemode`)と `GAMEMODE_SWITCHER`(F3+F4・F3+N)のときは `cancelMessage` に `spectator-denied` を入れる(コマンドを実行した人に表示される。他人に `/gamemode` を使った場合はその実行者に出る)。それ以外(他プラグインの `setGameMode` など)は本人に送る
  - 既に観戦モードの人(参加時・ワールドの移動時など)には何もしない。切り替えの瞬間だけを止める
- **観戦メニューのテレポート(`PlayerTeleportEvent`、LOW、取り消し済みは無視)**: `block-spectator-teleport` が true で、原因が `SPECTATE`(観戦メニューでほかのプレイヤーの所へ飛ぶ)のときだけ対象にする。行き先のワールドが制限の対象で、`spectatorlock.teleport` を持たない人なら取り消して `teleport-denied` を本人に送る。`/tp` などのコマンドのテレポートは止めない
- **`/spectatorlock reload`**: `spectatorlock.reload` が必要(コンソールも可)。`reloadConfig()` のあと起動時と同じように設定とメッセージを読み直す。引数が違えば `usage` を返す。Tab 補完は `reload` のみ
- **権限**: `spectatorlock.bypass`・`spectatorlock.teleport`・`spectatorlock.reload`。すべて既定は OP(`default: op`)
- データはファイルに保存しない

## 過去にハマった点(他プロジェクトでの経験)

- `config.getString(path, "")` のように既定値を渡すと、jar 内 config.yml の既定値が参照されない。既定値なしで取得して null を判定すること
- plugin.yml で `default: true` にした権限でも、登録されないと Bukkit は「OP のみ」として扱う。全員向けの機能を権限で縛らない

## ファイル構成

- `src/main/java/space/gorogoro/spectatorlock/SpectatorLock.java` — メインクラス。ゲームモード変更とテレポートの判定、`/spectatorlock reload`、設定の読み込みをすべて持つ
- `src/main/resources/plugin.yml` — プラグイン定義(コマンドと権限)
- `src/main/resources/config.yml` — ワールド限定、観戦メニューのテレポートの禁止、メッセージの言語(`language`)と英語・日本語のメッセージ(`messages`)
- `build.gradle.kts` / `settings.gradle.kts` / `gradle.properties` — Gradle の設定(バージョンは `gradle.properties`)
