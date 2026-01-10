# King's Line - コマンド一覧

## メインコマンド

### `/kl` または `/kingsline`

| コマンド | 権限 | 説明 |
|----------|------|------|
| `/kl help` | なし | ヘルプを表示 |
| `/kl info` | なし | 現在のゲーム状態を表示 |

---

## ゲーム管理コマンド

| コマンド | 権限 | 説明 |
|----------|------|------|
| `/kl start` | `kingsline.admin` | ゲームを開始 |
| `/kl stop` | `kingsline.admin` | ゲームを強制終了 |
| `/kl reload` | `kingsline.admin` | 設定をリロード |

---

## アリーナ設定コマンド

### 基本設定

| コマンド | 説明 |
|----------|------|
| `/kl createarena <名前>` | 新しいアリーナを作成 |
| `/kl setlobby` | ロビー位置を設定 |

### チーム設定

| コマンド | 説明 |
|----------|------|
| `/kl setspawn blue` | Blueチームのスポーン位置を設定 |
| `/kl setspawn red` | Redチームのスポーン位置を設定 |
| `/kl setcore blue` | Blueチームのコア位置を設定（見ている先のブロック） |
| `/kl setcore red` | Redチームのコア位置を設定（見ている先のブロック） |
| `/kl setnpc blue` | Blueチームのショップ/銀行NPC位置を設定 |
| `/kl setnpc red` | Redチームのショップ/銀行NPC位置を設定 |

### エリア設定

| コマンド | 説明 |
|----------|------|
| `/kl setarea B pos1` | Bエリアの角1を設定 |
| `/kl setarea B pos2` | Bエリアの角2を設定 |
| `/kl setarea A pos1` | Aエリアの角1を設定 |
| `/kl setarea A pos2` | Aエリアの角2を設定 |
| `/kl setarea C pos1` | Cエリアの角1を設定 |
| `/kl setarea C pos2` | Cエリアの角2を設定 |

### 保存

| コマンド | 説明 |
|----------|------|
| `/kl save` | 現在の設定を arenas.yml に保存 |

---

## パーティーコマンド

| コマンド | 説明 |
|----------|------|
| `/kl party invite <プレイヤー>` | プレイヤーをパーティーに招待 |
| `/kl party accept` | パーティー招待を承諾 |
| `/kl party deny` | パーティー招待を拒否 |
| `/kl party leave` | パーティーを離脱 |
| `/kl party list` | パーティーメンバーを表示 |
| `/kl party kick <プレイヤー>` | パーティーからキック（リーダーのみ） |
| `/kl party disband` | パーティーを解散（リーダーのみ） |

---

## ゲーム中コマンド（チャット）

| コマンド | 説明 |
|----------|------|
| `!king` | キングに立候補（準備フェーズ中のみ） |

---

## 権限

| 権限 | 説明 |
|------|------|
| `kingsline.admin` | 管理者コマンド（start, stop, reload, 設定系） |
| `kingsline.play` | ゲームに参加（デフォルトで全員に付与） |

---

## アリーナ設定の流れ

1. 新しいアリーナを作成
   ```
   /kl createarena myarena
   ```

2. 両チームのスポーン位置を設定
   ```
   /kl setspawn blue
   /kl setspawn red
   ```

3. 両チームのコアを設定（見ている先の黒曜石）
   ```
   /kl setcore blue
   /kl setcore red
   ```

4. 両チームのNPC位置を設定
   ```
   /kl setnpc blue
   /kl setnpc red
   ```

5. エリアを設定（Bは必須、A/Cは大規模用）
   ```
   /kl setarea B pos1
   /kl setarea B pos2
   ```

6. ロビーを設定（オプション）
   ```
   /kl setlobby
   ```

7. 設定を保存
   ```
   /kl save
   ```

8. ゲーム開始
   ```
   /kl start
   ```
