# King's Line - コマンド一覧

## プレイヤーコマンド

### /kl (メインコマンド)
メインコマンド。サブコマンドなしでヘルプを表示。

```
/kl
```

---

### /kl stats [player]
プレイヤーの統計を表示。

```
/kl stats           - 自分の統計を表示
/kl stats Player    - 指定プレイヤーの統計を表示
```

**出力例:**
```
========== PlayerName's Stats ==========
Wins: 15
Kills: 234
Deaths: 189
K/D Ratio: 1.24
=========================================
```

---

### /kl party
パーティー関連のコマンド。

```
/kl party invite <player>  - プレイヤーをパーティーに招待
/kl party accept           - パーティー招待を承諾
/kl party deny             - パーティー招待を拒否
/kl party leave            - パーティーを離脱
/kl party list             - パーティーメンバーを表示
/kl party disband          - パーティーを解散 (リーダーのみ)
```

---

### /kl team
チーム関連のコマンド（デバッグ/テスト用）。

```
/kl team info              - 自分のチーム情報を表示
```

---

## 管理者コマンド

### /kl start
ゲームを開始する。

```
/kl start
```

**必要権限:** `kingsline.admin`

---

### /kl stop
ゲームを強制終了する。

```
/kl stop
```

**必要権限:** `kingsline.admin`

---

### /kl reload
設定ファイルをリロードする。

```
/kl reload
```

**必要権限:** `kingsline.admin`

---

## セットアップコマンド

アリーナの設定を行うコマンド群。

### /kl setspawn <team>
チームのスポーン位置を設定。

```
/kl setspawn blue          - Blueチームのスポーン
/kl setspawn red           - Redチームのスポーン
```

**必要権限:** `kingsline.admin`

---

### /kl setcore <team>
チームのコア位置を設定。

```
/kl setcore blue           - Blueチームのコア
/kl setcore red            - Redチームのコア
```

**必要権限:** `kingsline.admin`

---

### /kl setnpc <team>
チームのNPC位置を設定（ショップ/Shard銀行）。

```
/kl setnpc blue            - BlueチームのNPC
/kl setnpc red             - RedチームのNPC
```

**必要権限:** `kingsline.admin`

---

### /kl setarea <area> <pos1|pos2>
エリアの範囲を設定。

```
/kl setarea A pos1         - Aエリアの角1
/kl setarea A pos2         - Aエリアの角2
/kl setarea B pos1         - Bエリアの角1
/kl setarea B pos2         - Bエリアの角2
/kl setarea C pos1         - Cエリアの角1
/kl setarea C pos2         - Cエリアの角2
```

**必要権限:** `kingsline.admin`

---

### /kl setlobby
ロビー位置を設定（ゲーム終了後のテレポート先）。

```
/kl setlobby
```

**必要権限:** `kingsline.admin`

---

### /kl save
現在の設定を `arenas.yml` に保存。

```
/kl save
```

**必要権限:** `kingsline.admin`

---

### /kl createarena <name>
新しいアリーナを作成。

```
/kl createarena castle     - "castle"という名前のアリーナを作成
```

**必要権限:** `kingsline.admin`

---

### /kl setarena <name>
使用するアリーナを切り替え。

```
/kl setarena castle        - "castle"アリーナを使用
/kl setarena desert        - "desert"アリーナを使用
```

**必要権限:** `kingsline.admin`

---

## デバッグコマンド

開発/テスト用のコマンド。

### /kl debug
デバッグモードの切り替え/情報表示。

```
/kl debug                  - デバッグモード切り替え
/kl debug info             - 現在のゲーム状態を表示
/kl debug shard <amount>   - Shardを自分に付与
/kl debug lumina <amount>  - Luminaを自分に付与
/kl debug sp               - SPゲージを満タンにする
/kl debug king             - 自分をキングにする
```

**必要権限:** `kingsline.admin`

---

## 権限一覧

| 権限 | 説明 | デフォルト |
|------|------|-----------|
| `kingsline.play` | ゲームに参加できる | true |
| `kingsline.stats` | 統計を見る | true |
| `kingsline.party` | パーティー機能を使う | true |
| `kingsline.admin` | 管理者コマンドを使用 | op |

---

## エイリアス

`/kl` の代わりに以下も使用可能:
- `/kingsline`
- `/kingline`
