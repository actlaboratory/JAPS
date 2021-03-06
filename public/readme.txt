		Jpki Accessible pdf signer -JAPS-

	バージョン:　　ver.1.0.0
	リリース:　　　2022-03-21
	開発・配布元:　ACT Laboratory　(https://actlab.org/)
	主要開発者:　　吹雪
　　ソフト種別:　　オープンソースソフトウェア　(GitHubリポジトリ:https://github.com/actlaboratory/JAPS/)
　　ライセンス:　　MIT License


第１章　はじめに
１．１　本ソフトウェアの概要
本ソフトウェアは、HIRUKAWA Ryo氏が提供するオープンソースソフトウェア「JPKI PDF SIGNER」を、
スクリーンリーダーおよびキーボードを用いた操作で利用できるように修正したものです。

本ソフトウェアを用いることで、独立行政法人地方公共団体情報システム機構(JLIS)が運営する公的個人認証サービス(JPKI)による電子証明書を用いてPDFファイルに電子署名を施すことができます。
署名したPDFファイルは、本人が作成・承認等したことを証明できるとともに、法人設立登記等の際に使用することができます。
株式会社等の設立時に必要な定款に貼付する印紙代約４万円を節約できることなどから、少しずつ利用が広がっています。

その他にも、電子署名及び認証業務に関する法律第２条に基づき、一定の電子署名がなされた文書は、本人が署名または押印した紙の文書と同様、真正に成立したものと推定されます。
当団体では、オンラインで開催する総会の議事録に、議長と他の出席者１名が電子署名したものを保管することで、議事内容の記録の正確性を担保することにしています。


１．２　動作環境について
64ビット版Windows10で動作確認を行っています。
多くの場合、Windowsの他のバージョンでも動作すると思われますが、32ビット版のWindowsでは動作しないことにご注意ください。

画面上に表示された文字などの情報は、Java Access Bridge(JAB)を通じてスクリーンリーダーで読み上げ可能です。
JABに対応したスクリーンリーダーとして、国内では、NVDA及びJAWSが有名です。
PC-Talkerでは読み上げできないことにご注意ください。

署名を行うにはJLISが配布する「JPKI利用者ソフト」のインストールと、このソフトでの署名に対応したICカードリーダーの接続が必要です。
このソフトの動作環境や対応するICカードリーダーについては、JLISのウェブサイトを参照してください。
なお、JAPSを起動して署名を試みた際にJPKI利用者ソフトがインストールされていない場合には、ダウンロード画面をブラウザで開く機能が搭載されていますので、セットアップにご活用ください。


第２章　セットアップ
２．１　インストール
使用するにあたって、特別なインストール作業は必要ありません。
ダウンロードしたzipファイルを適当な場所に展開し、「JAPS」フォルダ内の「JAPS.exe」を実行すると起動します。

２．２　アンインストール
コンピューターから削除する場合は、「JAPS」フォルダを削除してください。
その他、特別な作業は不要です。

２．３　アップデート
発見された不具合の修正、新機能の追加、JPKI利用者ソフトの仕様変更への対応などの目的で、バージョンアップを行う場合があります。
新しいバージョンの有無は、メニューバーの[ヘルプ]→[更新を確認]から、オンラインで確認が可能です。


第３章　基本操作
この章では、JAPSの基本操作を説明します。

３．１　事前準備
JPKI利用者ソフトのインストール、対応するカードリーダーの接続、カードリーダーへのカードのセットを済ませておいてください。

３．２　PDFファイルを開く
JAPS.exeを実行したら、起動するまで３～10秒くらいかかりますので、しばらくお待ちください。
起動が完了したら、メニューバーから[ファイル]→[開く]を実行して、表示されたウィンドウからPDFファイルを選択して開いてください。

なお、本ソフトではメニューバーの操作がうまくいかないときがあるようです。
いつもの操作でうまくメニューが開かないと思ったら、[Alt]キーを押した後、[TAB]キーを押してみてください。

３．３　不可視署名の実行
[TAB]キーで移動すると、幾つかのボタンと、署名の種類を選択できるリストがあります。
リスト上で、[印影なしで電子署名する]を選択し、[Enter]キーを押すと、署名を実行できます。

署名には署名用電子証明書の暗証番号(半角大文字及び数字で構成)が必要です。連続５回間違えるとロックされるので注意してください。

３．４　保存

署名したPDFファイルは、メニューバーで[ファイル]→[名前を付けて保存]または[上書き保存]を実行して保存してください。


第４章　連絡先
このソフトウェアを利用しての感想やご要望、不具合のご報告などは、以下のメールアドレスまたは掲示板へご連絡ください。

ACT Laboratory サポート：support@actlab.org
ACT Laboratory 掲示板：https://actlab.org/bbs/
ACT Laboratory Twitter：https://twitter.com/act_laboratory
