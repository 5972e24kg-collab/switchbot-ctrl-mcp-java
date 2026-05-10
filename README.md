# SwitchBot Control MCP Java Server Public Sample

## 1. 概要

本リポジトリは、SwitchBot の Scene を LLM から安全に操作するために作成した、Java 製 MCP Server の公開用サンプルです。

個人開発環境で運用している Java MCP Server から、公開可能な範囲を抽出し、単独でビルド・確認できる形に整理したものです。

主な目的は、以下の技術要素を示すことです。

- Java Servlet による MCP / JSON-RPC 風のサーバー実装
- `initialize` / `tools/list` / `tools/call` の基本的な処理構造
- SwitchBot Cloud API v1.1 への署名付きリクエスト実装
- LLM に SwitchBot デバイスを直接触らせず、Scene 実行に限定する設計
- `sceneCatalog.json` による公開許可済み Scene の管理
- `sceneKey` による安全なシーン実行
- cooldown / dry-run を含む、物理世界操作向けの安全制御
- 個人開発コードから公開可能な最小単位を切り出す際のマスク・公開方針

このリポジトリは、完成品の汎用ライブラリではなく、AI / LLM システム開発ポートフォリオの一部として公開している実装サンプルです。

## 2. このリポジトリで示したいこと

本実装の主題は、単に SwitchBot API を MCP Server から呼び出すことではありません。

重要なのは、LLM に対して SwitchBot のデバイス操作権限を直接渡さず、あらかじめ人間が許可した Scene だけを実行可能にしている点です。

SwitchBot には、大きく分けて以下の2種類の操作対象があります。

1. Device  
   SwitchBot に登録されている個別デバイス

2. Scene  
   SwitchBot アプリ上でユーザーが任意に定義した操作セット

本実装では、MCP から Device を直接操作しません。

理由は、Device 直接操作を許可すると、LLM が触るべきではない家電や、意図しない部屋・機器を操作する可能性があるためです。

そのため本実装では、以下の責任分界にしています。

```text
LLM:
  ユーザー意図に合う sceneKey を選ぶ

MCP Server:
  sceneKey が公開許可済みか確認する
  cooldown / dry-run を適用する
  対応する sceneId を解決する

SwitchBot App:
  Scene の中身として、実際にどの家電をどう動かすかを管理する
```

この構成により、LLM に対して「家電を直接操作させる」のではなく、「人間が事前に許可した Scene を選ばせる」設計にしています。

## 3. 公開範囲について

本リポジトリは、個人開発環境で運用中のコードをそのまま公開したものではありません。

公開にあたり、以下の情報は含めていません。

* SwitchBot API Token
* SwitchBot API Secret
* 実運用している sceneId
* 個人宅の構成を特定できる Scene 名
* 実IPアドレス
* 実ホスト名
* 個人開発環境用の設定ファイル
* 個人開発環境の運用ログ
* 自作共通ライブラリ全体
* 検証用の個人向け Servlet

一方で、MCP Server としての構造、SwitchBot Scene の扱い方、LLM に物理世界を操作させる際の責任分界、安全制御の考え方が分かる範囲は残しています。

## 4. 注意事項

このリポジトリは、公開用に抽出したサンプルです。

本番環境でそのまま利用することは想定していません。

特に、以下の点には注意してください。

* CORS 設定はローカル検証向けです
* 認証・認可は簡略化されています
* SwitchBot API Token / Secret は必ず環境変数などで安全に渡してください
* Device 直接操作は実装していません
* Scene 実行は実際の家電操作につながる可能性があります
* 実運用する場合は、接続元 Origin、認証、ログ出力、例外メッセージ、Token 管理、Scene 公開範囲を環境に合わせて見直してください
* LLM に Tool Use させる場合は、必ず検証環境または dry-run で動作確認してください

## 5. 提供する MCP Tools

本サンプルでは、以下の2つの Tool を提供します。

### 5.1 scenes

MCP Server から利用可能な SwitchBot Scene の一覧を返します。

想定用途:

* 利用可能な Scene の確認
* `executeScene` に渡す `sceneKey` の確認
* ユーザーが「何ができる？」と聞いた場合の候補提示
* 実行前に、LLM が操作可能な Scene を確認する

この Tool は、SwitchBot アプリに存在する全 Scene を返すものではありません。

`sceneCatalog.json` に登録され、かつ SwitchBot API 側にも存在する、公開許可済みの Scene だけを返します。

### 5.2 executeScene

指定した `sceneKey` の SwitchBot Scene を実行します。

想定用途:

* 照明をつける
* 照明を消す
* 照明モードを変更する
* 空調停止など、事前に SwitchBot アプリ側で定義した Scene を実行する

この Tool は `sceneId` を直接受け取りません。

LLM は、`scenes` Tool で返された `sceneKey` を指定します。

```text
OK:
  sceneKey = room_light_off

NG:
  sceneId = T02-xxxxxxxxxxxx-xxxxxxxx
  自然言語 = 照明消して
  alias = おやすみ
```

自然言語や alias から `sceneKey` を選ぶ処理は、LLM 側または上位の制御層で行う想定です。

## 6. ディレクトリ構成

```text
src/main/java/vr46/switchbotctrlmcppublic/
  config/
    AppConfig.java
    
  converter/
    SwitchBotScenesResponse.java
    SwitchBotScenesConverter.java
    SwitchBotScenesConvertException.java

  dto/
    SceneSummary.java

  mcp/
    BaseMcpServlet.java
    InitializeRequest.java
    InitializeResponse.java
    ToolsListRequest.java
    ToolsListResponse.java
    ToolsCallRequest.java
    ToolsCallResponse.java

  web/
    McpServlet.java
    
  scene/
    SceneCatalog.java
    SceneCatalogEntry.java
    SceneCatalogException.java
    SceneService.java
    SceneServiceException.java
    SceneExecutionResult.java

  switchbot/
    SwitchBotApisV2.java
    SwitchBotApiResult.java
    SwitchBotApiException.java

  logging/
    MyLogger.java

src/main/resources/
  sceneCatalog.json
```

公開版では、実装都合により package や配置が上記と完全に一致しない場合があります。

## 7. 主なクラスの役割

### 7.1 BaseMcpServlet

MCP Server としての共通処理を担当します。

主な責務:

* HTTP POST の受付
* JSON-RPC 形式の検証
* `initialize` の処理
* `notifications/initialized` の処理
* `tools/list` の処理
* `tools/call` の処理
* MCP session ID の発行・検証
* 基本的なエラーレスポンス生成
* CORS ヘッダー付与

このクラスは、個別の Tool 実装に依存しない MCP 共通基盤として作成しています。

### 7.2 McpServlet

SwitchBot Scene 制御用 Tool を定義する Servlet です。

主な責務:

* `scenes` Tool の定義
* `executeScene` Tool の定義
* LLM 向け instructions の設定
* Tool 呼び出しを `SceneService` へ委譲
* Tool 実行結果を MCP response として返却
* Tool 内部エラーを `isError=true` の Tool error として返却

`McpServlet` は SwitchBot API を直接呼び出しません。

Scene の取得、公開可否、cooldown、dry-run、実行制御は `SceneService` 以下の層に委譲します。

### 7.3 SwitchBotApisV2

SwitchBot Cloud API v1.1 への低レイヤー通信を担当します。

主な責務:

* API Token / Secret を使った署名生成
* `GET /v1.1/devices`
* `GET /v1.1/scenes`
* `POST /v1.1/scenes/{sceneId}/execute`
* HTTP status code と response body の保持
* 通信不能・署名失敗・URI生成失敗などの例外化

HTTP 4xx / 5xx は例外ではなく、`SwitchBotApiResult` として返す方針です。

通信そのものが成立しなかった場合は、`SwitchBotApiException` として扱います。

### 7.4 SceneCatalog

`src/main/resources/sceneCatalog.json` を読み込み、人間が管理する公開用 Scene 定義を返します。

主な責務:

* `sceneCatalog.json` の classpath resource 読み込み
* JSON形式の検証
* 必須項目の検証
* `sceneKey` 重複検出
* `sceneId` 重複検出
* `cooldown` 値の検証

`SceneCatalog` は SwitchBot API を呼び出しません。

### 7.5 SceneCatalogEntry

`sceneCatalog.json` の1件分を表す内部管理用DTOです。

代表項目:

* `sceneKey`
* `sceneId`
* `sceneName`
* `publicName`
* `category`
* `action`
* `riskLevel`
* `executable`
* `aliases`
* `cooldown`
* `note`

`note` は人間の管理用メモであり、LLM / MCP へ返す DTO には含めません。

### 7.6 SceneSummary

LLM / MCP に正式公開する Scene DTO です。

代表項目:

* `sceneKey`
* `sceneId`
* `sceneName`
* `publicName`
* `category`
* `action`
* `riskLevel`
* `executable`
* `aliases`
* `cooldown`

`SceneCatalogEntry` に含まれる `note` は、`SceneSummary` には含めません。

### 7.7 SwitchBotScenesConverter

SwitchBot API の `getScenes()` raw JSON と `SceneCatalogEntry` 一覧を突合し、`SceneSummary` 一覧へ変換します。

変換方針:

```text
SwitchBot API の getScenes に存在する
かつ
sceneCatalog.json に存在する
かつ
sceneId が一致する
かつ
executable = true
```

この条件を満たす Scene だけを公開対象とします。

これにより、SwitchBot アプリ側に Scene を追加しただけでは、MCP には自動公開されません。

### 7.8 SceneService

MCP Tool から呼ばれる中心サービスです。

主な責務:

* `List<SceneSummary>` の取得
* `List<SceneSummary>` の24時間キャッシュ
* `refreshScenes()` による手動更新
* `sceneKey` による Scene 実行
* cooldown 制御
* dry-run 制御
* `SceneExecutionResult` の生成

MCP Tool として公開するのは以下のみです。

* `listScenes()`
* `executeBySceneKey(sceneKey)`

`refreshScenes()` は保守用メソッドであり、MCP Tool としては公開しません。

### 7.9 SceneExecutionResult

Scene 実行結果を表すDTOです。

代表項目:

* `sceneKey`
* `sceneId`
* `sceneName`
* `success`
* `httpStatusCode`
* `switchBotBody`
* `message`
* `executed`
* `dryRun`

`executed` は、実際に SwitchBot API の executeScene を呼んだかを表します。

`dryRun` は、dry-run により物理実行をスキップしたかを表します。

## 8. sceneCatalog.json

`sceneCatalog.json` は、LLM / MCP に公開してよい Scene を人間が管理するための設定ファイルです。

例:

```json
[
  {
    "sceneKey": "room_light_full",
    "sceneId": "sample-scene-id-001",
    "sceneName": "照明全灯",
    "publicName": "部屋の照明を全灯する",
    "category": "light",
    "action": "set_mode",
    "riskLevel": "safe",
    "executable": true,
    "aliases": ["明るくして"],
    "cooldown": 5,
    "note": "公開用サンプル"
  },
  {
    "sceneKey": "room_light_off",
    "sceneId": "sample-scene-id-002",
    "sceneName": "照明消灯",
    "publicName": "部屋の照明を消す",
    "category": "light",
    "action": "turn_off",
    "riskLevel": "safe",
    "executable": true,
    "aliases": ["照明消して", "おやすみ"],
    "cooldown": 1,
    "note": "公開用サンプル"
  }
]
```

### 8.1 sceneKey

MCP Tool から Scene を指定するための公開キーです。

LLM は `sceneId` ではなく `sceneKey` を使います。

### 8.2 sceneId

SwitchBot API 側で Scene 実行に必要なIDです。

公開版では実値を含めず、サンプル値に置き換えています。

### 8.3 publicName

LLMや人間が理解しやすい説明名です。

### 8.4 aliases

ユーザー発話と Scene を対応づけるための補助情報です。

本サンプルでは alias マッチングは実装していません。

### 8.5 cooldown

同じ `sceneKey` を連続実行しないための待機時間です。

単位は秒です。

```json
"cooldown": 5
```

* 未設定または `null`: デフォルト10秒
* `0`: 連続実行を許可
* 負数: エラー
* 非数値: エラー

### 8.6 note

人間の管理用メモです。

`SceneSummary` には含めず、LLM / MCP には返しません。

## 9. 設定

本プロジェクトでは、SwitchBot API Token / Secret をソースコードに直書きしません。

必要な値は環境変数として渡します。

公開用の設定例は `.env.example` に記載します。

```env
SWITCHBOT_TOKEN=replace-with-your-token
SWITCHBOT_SECRET=replace-with-your-secret
MCP_PROTOCOL_VERSION=2025-06-18
```

ローカルで `.env` を使う場合は、以下のようにコピーして使用します。

```bash
cp .env.example .env
```

ただし、Java は標準では `.env` を自動読み込みしません。

IntelliJ IDEA、Tomcat、Docker Compose、シェルなど、実行環境側で環境変数として渡してください。

`.env` には秘密情報やローカル環境固有の値が入るため、Git 管理対象外にしています。

## 10. ビルド

Maven Wrapper を使用します。

Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

Linux / macOS:

```bash
./mvnw clean package
```

ビルド成果物は `target/` 配下に生成されます。

## 11. 実行方法

このプロジェクトは Java Servlet ベースの Web アプリケーションです。

Tomcat などの Servlet Container にデプロイして使用します。

例:

```text
http://localhost:8080/switchbot-ctrl-mcp-java/mcp
```

実際の URL は、デプロイ先のコンテキストパスにより変わります。

## 12. MCP 呼び出し例

### 12.1 initialize

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-06-18",
    "capabilities": {},
    "clientInfo": {
      "name": "sample-client",
      "version": "0.1.0"
    }
  }
}
```

Response では、`Mcp-Session-Id` ヘッダーが返ります。

以降の `tools/list` や `tools/call` では、この session ID を `Mcp-Session-Id` ヘッダーに指定します。

### 12.2 notifications/initialized

Request:

```json
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized",
  "params": {}
}
```

### 12.3 tools/list

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

Response には、利用可能な Tool と input schema が含まれます。

### 12.4 tools/call: scenes

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "scenes",
    "arguments": {}
  }
}
```

Response の `content[0].text` には、公開可能な Scene 一覧が JSON 文字列として入ります。

例:

```json
[
  {
    "sceneKey": "room_light_full",
    "sceneId": "sample-scene-id-001",
    "sceneName": "照明全灯",
    "publicName": "部屋の照明を全灯する",
    "category": "light",
    "action": "set_mode",
    "riskLevel": "safe",
    "executable": true,
    "aliases": ["明るくして"],
    "cooldown": 5
  }
]
```

### 12.5 tools/call: executeScene

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "executeScene",
    "arguments": {
      "sceneKey": "room_light_off"
    }
  }
}
```

Response の `content[0].text` には、Scene 実行結果が JSON 文字列として入ります。

例:

```json
{
  "sceneKey": "room_light_off",
  "sceneId": "sample-scene-id-002",
  "sceneName": "照明消灯",
  "success": true,
  "httpStatusCode": 200,
  "switchBotBody": "{\"statusCode\":100,\"body\":{},\"message\":\"success\"}",
  "message": "Scene executed.",
  "executed": true,
  "dryRun": false
}
```

## 13. 設計上の特徴

### 13.1 LLM に Device を直接操作させない

SwitchBot API は Device 操作も可能ですが、本実装では Device 操作を公開していません。

LLM から物理世界を操作する場合、対象の曖昧さが問題になります。

例えば「照明をつけて」という発話だけでは、以下が曖昧です。

* どの部屋の照明か
* どの明るさにするのか
* どのデバイスを操作するのか
* 他の家電も連動するのか

本実装では、こうした判断を SwitchBot アプリ側の Scene に閉じ込めています。

MCP Server は、あくまで公開許可済みの Scene を実行するだけです。

### 13.2 sceneCatalog.json による公開制御

SwitchBot アプリ側に Scene を追加しても、自動的には MCP に公開されません。

公開するには、`sceneCatalog.json` に登録する必要があります。

これにより、以下を防ぎます。

* LLMに触らせたくない Scene の露出
* 作成途中の Scene の誤実行
* SwitchBot アプリ側の変更が即座に MCP Tool に反映されること

### 13.3 sceneId ではなく sceneKey を使う

MCP Tool の `executeScene` は `sceneId` を受け取りません。

代わりに、人間が管理する `sceneKey` を受け取ります。

これにより、LLM に SwitchBot API の内部IDを直接扱わせずに済みます。

### 13.4 cooldown

同じ `sceneKey` の連続実行を抑制するため、cooldown を持たせています。

cooldown は Scene ごとに `sceneCatalog.json` で設定できます。

### 13.5 dry-run

`SceneService` は dry-run モードに対応しています。

dry-run 時は、Scene 解決までは行いますが、実際の SwitchBot API 実行は行いません。

これは、物理世界へ介入する Tool Use の検証時に有効です。

### 13.6 Tool 数を最小化

本実装で公開する Tool は以下の2つだけです。

```text
scenes
executeScene
```

`refreshScenes` は内部保守用メソッドであり、MCP Tool には公開していません。

これは、小型 LLM でも Tool 選択を安定させるための設計です。

## 14. セキュリティ方針

本リポジトリでは、以下の方針で公開しています。

* SwitchBot API Token はコミットしない
* SwitchBot API Secret はコミットしない
* `.env` はコミットしない
* `.env.example` のみ公開する
* 実sceneIdは記載しない
* 個人宅の構成を特定できる Scene 名は記載しない
* 実IPアドレスは記載しない
* 実ホスト名は記載しない
* 内部環境を推測できるログは記載しない
* Device 直接操作 Tool は提供しない
* Scene 実行に限定する
* `sceneCatalog.json` に登録された Scene だけを公開する

## 15. このリポジトリの位置づけ

このリポジトリは、以下の技術ポートフォリオの一部です。

* ローカル LLM 基盤
* RAG / ETL
* QLoRA
* 自律型AIエージェント
* MCP / Tool Use
* AI伴走型開発
* LLMによる物理世界操作の安全設計

その中で本リポジトリは、特に MCP / Tool Use と、LLM による外部システム操作の安全設計を示す実装証跡として位置づけています。

Portainer Ops MCP Java Server が「LLMに外部システムを観測させる」サンプルであるのに対し、本リポジトリは「LLMに物理世界へ限定的に介入させる」サンプルです。

ただし、本実装では Device 直接操作を避け、人間が事前に許可した Scene のみを操作対象にしています。

## 16. License

* MIT License

## 17. Author

5972e24kg-collab
