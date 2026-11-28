# SleepStore

日本語 · [English](README.md)

Galaxyの**ディープスリープ中のアプリ**を、ディープスリープのままGoogle PlayストアやGalaxy Storeで更新確認できるようにします。Vectorで選んだストアにだけ対象アプリが表示され、Samsungのバックグラウンド制限と、手動で無効にしたアプリの状態は維持されます。

[署名済みAPKをダウンロード](https://github.com/OneUI-Mods/SleepStore/releases/latest) · [不具合を報告](https://github.com/OneUI-Mods/SleepStore/issues/new)

## 対応環境

| 項目 | 対応内容 |
| --- | --- |
| 端末 | Samsungの自動無効化APIを搭載したAndroid 16または17のGalaxy |
| フレームワーク | Vectorなど、Xposed API 102に対応したもの |
| 実機で確認したストア | Galaxy S26 UltraのGoogle PlayストアとGalaxy Store |
| ほかのアプリ | Aurora Store、Obtainiumなどのストアや更新アプリも対象に選べます。 |

確認したファームウェア、ストアのバージョン、検証内容は[実機検証の記録](docs/validation.md)を参照してください。

## 初回設定

1. [GitHub Releases](https://github.com/OneUI-Mods/SleepStore/releases/latest)から署名済みAPKをインストールします。
2. Vectorで**SleepStore**を有効にし、利用するストアをモジュールの適用範囲に追加します。複数のAndroidプロファイルを使う場合は、プロファイルごとに設定します。
3. 対象ストアを一度強制停止して開き直し、普段どおり更新を確認します。

設定画面に接続状態を表示するには、適用範囲に**SleepStore自身**も追加します。「接続済み」は、Vectorが設定アプリのプロセスでSleepStoreモジュールを読み込んだことを示します。ストアの適用範囲は、同じプロファイルで別途確認してください。

## 更新・停止

- **Android 17で0.2.4以前から更新する場合**。APKを上書きしたあと、対象ストアを一度強制停止して開き直します。
- **ホット更新**。0.2.5から0.2.6への更新では、Google PlayストアとGalaxy Storeを開き直さずに反映できることを確認済みです。旧版の動作が続く場合は、対象ストアを一度開き直してください。
- **停止する場合**。VectorでSleepStoreを無効にしてから、対象ストアを強制停止して開き直します。

いずれも端末の再起動は不要です。更新の提供やインストールは、互換性や段階配信など、各ストアの条件に従います。Androidシステム、SystemUI、デバイスケアには引き続き端末標準の動作が適用されます。

## 自動無効化の修復

Android 17のGalaxyでは、**ディープスリープを修復 → 修復対象を確認**から、Google PlayストアまたはGalaxy Storeの更新後にディープスリープの設定と実際の無効状態がずれたアプリを調べられます。root権限で現在のプロファイルを確認し、一覧を承認すると、停止中の対象アプリを自動無効化の状態に戻します。

## 開発・ライセンス

[開発者向けガイド](docs/development.md) · [ビルド環境](docs/build-environment.md) · [変更履歴](CHANGELOG.md)

[MIT License](LICENSE) · [第三者ライセンス](THIRD_PARTY_NOTICES.md) · [OneUI Mods](https://github.com/OneUI-Mods)
