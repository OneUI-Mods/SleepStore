# 配布手順

JDK 21、Android SDK／Build Tools 37.0.0を使用します。compileSdkは37、targetSdkは36の既存設定を維持します。署名済みrelease APKだけを配布し、debug・検証用Activityを含むAPKは配布しません。

既存の非公開signing.propertiesと同じ配布鍵を使います。鍵・パスワードをGit、ログ、ソースZIPへ含めません。

1. ソースと利用者向けの説明をコミットし、作業ツリーをcleanにします。
2. `python3 tools/package_release.py --source-ref HEAD`を実行します。
3. releaseビルド、JVM試験、lint、API 102とBinder provider、検証用コンポーネント不在、alignment、既存証明書との一致が成功したことを確認します。
4. `artifacts/release/<version>/`のAPK、同じコミットのソースZIP、SHA256SUMS、署名検証結果を配布します。

公開タイトルと説明は利用者向けに対応環境・変更点・更新操作・制限を記載します。実装・試験記録と実機情報は分離し、既存タグは移動しません。
