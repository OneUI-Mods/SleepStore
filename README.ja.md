# SleepStore

[English](README.md) · 日本語

ディープスリープ中のGalaxyアプリを、ディープスリープから外さずに選択したストアへ表示するVectorモジュールです。

## 導入・使い方

1. [Releases](https://github.com/OneUI-Mods/SleepStore/releases)から署名済みrelease APKをインストールします。
2. API 102対応Vectorなどでモジュールを有効にします。
3. 使用するストアを対象に選びます。Google Play、Galaxy Store、Aurora Store、Obtainiumなどを選択できます。
4. ストアを強制停止して開き直し、更新を確認します。

確認済みの基準はSamsungの自動無効化APIを持つAndroid 16です。Androidシステム、SystemUI、デバイスケアは対象外です。プロファイルごとに設定してください。SleepStore自身の選択は接続表示用で、ストアの対象設定を代わりに行うものではありません。

## 制限・停止

更新の提供、互換性、段階配信、インストールの判断はストア側で行います。表示されても必ず更新できるわけではありません。ストア候補の一覧は、すべてを実機保証した一覧ではありません。

OSのディープスリープ状態を書き換えません。無効にする場合はモジュールを停止してストアを再起動します。選択外のアプリや外部のroot・Shizuku操作、OS自体の変更は保護の対象外です。

## 開発・ライセンス

[開発者向けガイド](docs/development.md)、[MIT License](LICENSE)、[第三者ライセンス](THIRD_PARTY_NOTICES.md)を参照してください。

[OneUI Modsのアプリ一覧](https://github.com/OneUI-Mods)
