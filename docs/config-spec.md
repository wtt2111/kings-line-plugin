# King's Line - 設定ファイル仕様

## config.yml

ゲーム全体の設定を管理します。

```yaml
# ========================================
# King's Line - Configuration
# ========================================

# ゲーム設定
game:
  # 最小プレイヤー数 (これ以上でゲーム開始可能)
  min-players: 6
  
  # 準備フェーズの時間 (秒)
  starting-phase-duration: 45
  
  # リスポーン時間 (秒)
  respawn-time: 5
  
  # 勝利に必要なポイント (相手のリスポーン無効化)
  points-to-win: 500
  
  # 小規模モードの閾値 (1チームあたりの人数)
  # これ以下ならBエリアのみ、超えたらA/B/C
  small-scale-threshold: 4

# スコア設定
score:
  # 通常キル
  kill: 4
  
  # キングキル
  king-kill: 20
  
  # キング死亡ペナルティ (倒されたチーム)
  king-death-penalty: -50
  
  # コア破壊
  core-destroy: 100
  
  # Bエリア占領 (tick毎)
  area-capture: 2
  
  # エリア占領の判定間隔 (tick = 20分の1秒, 60 = 3秒)
  area-tick-interval: 60

# Shard設定
shard:
  # Shardノードのスポーン間隔 (秒)
  spawn-interval: 10
  
  # 1回のスポーンで出る個数
  spawn-amount: 1
  
  # コア破壊時にドロップする数
  core-destroy-drop: 20
  
  # キング死亡時にドロップする数
  king-death-drop: 5

# Lumina設定
lumina:
  # キル時の獲得量
  per-kill: 2

# キング設定
king:
  # オーラの範囲 (ブロック)
  aura-radius: 8
  
  # キング死亡時の敵バフ時間 (秒)
  death-buff-duration: 15

# エレメント設定
element:
  # SP技のクールダウン (秒)
  sp-cooldown: 15
  
  # SP技に必要なヒット数
  sp-required-hits: 10

# データベース
database:
  # SQLiteファイル名
  filename: "stats.db"
```

---

## arenas.yml

アリーナ（マップ）の設定を管理します。

```yaml
# ========================================
# King's Line - Arena Configuration
# ========================================

# 現在使用するアリーナ
current-arena: "castle"

# アリーナ定義
arenas:
  castle:
    # ワールド名
    world: "world"
    
    # チームスポーン位置
    spawns:
      blue:
        x: 50.5
        y: 65.0
        z: 100.5
        yaw: 90.0
        pitch: 0.0
      red:
        x: 200.5
        y: 65.0
        z: 100.5
        yaw: -90.0
        pitch: 0.0
    
    # コア位置 (黒曜石)
    cores:
      blue:
        x: 40
        y: 65
        z: 100
      red:
        x: 210
        y: 65
        z: 100
    
    # NPC位置 (ショップ/Shard銀行)
    npcs:
      blue:
        x: 45.5
        y: 65.0
        z: 95.5
      red:
        x: 205.5
        y: 65.0
        z: 95.5
    
    # エリア定義
    areas:
      # 中央エリア (常に有効)
      B:
        pos1:
          x: 100
          y: 60
          z: 80
        pos2:
          x: 150
          y: 80
          z: 120
        # Shardスポーン位置
        shard-spawn:
          x: 125.5
          y: 65.0
          z: 100.5
      
      # 自陣寄りエリア (5v5以上で有効)
      A:
        pos1:
          x: 60
          y: 60
          z: 80
        pos2:
          x: 90
          y: 80
          z: 120
        shard-spawn:
          x: 75.5
          y: 65.0
          z: 100.5
      
      # 敵陣寄りエリア (5v5以上で有効)
      C:
        pos1:
          x: 160
          y: 60
          z: 80
        pos2:
          x: 190
          y: 80
          z: 120
        shard-spawn:
          x: 175.5
          y: 65.0
          z: 100.5
    
    # ロビー位置 (ゲーム終了後のテレポート先)
    lobby:
      x: 0.5
      y: 65.0
      z: 0.5
      yaw: 0.0
      pitch: 0.0
```

---

## チームアップグレード設定

現在はコード内で定義されています（`TeamUpgrade.java`）。

| アップグレード | Tier 1 | Tier 2 | Tier 3 |
|---------------|--------|--------|--------|
| 防具 | 鉄 (100) | ダイヤ (250) | - |
| 武器 | 石 (50) | 鉄 (120) | ダイヤ (250) |
| プロテクション | I (80) | II (160) | III (300) |
| シャープネス | I (80) | II (160) | III (300) |
| 生命力強化 | +2HP (150) | +4HP (350) | - |
| 俊足 | +10% (120) | +20% (280) | - |

---

## 個人ショップ設定

現在はコード内で定義されています（`ShopGUI.java`）。

### 消耗品
| アイテム | 価格 |
|----------|------|
| 金のリンゴ | 8 |
| 治癒のスプラッシュ | 5 |
| スピード II (30秒) | 6 |
| 跳躍 II (30秒) | 4 |
| 牛乳 | 3 |
| ステーキ x5 | 2 |

### 武器・ツール
| アイテム | 価格 |
|----------|------|
| 弓 | 10 |
| 矢 x16 | 4 |
| 火矢 x8 | 8 |
| 毒矢 x8 | 10 |
| 鉄の剣 (早期購入) | 25 |
| 鉄装備セット | 40 |

### 特殊アイテム
| アイテム | 価格 |
|----------|------|
| エンダーパール | 15 |
| ゴーストオーブ | 20 |
| フラッシュバン x3 | 6 |
| グラップル | 12 |
| ファイアチャージ x3 | 10 |
| シールドトーテム | 25 |
| デスマーク | 10 |

---

## コマンドでの設定

アリーナ設定はコマンドでも行えます：

```
/kl createarena <name> - 新しいアリーナを作成
/kl setspawn blue      - Blueチームのスポーンを設定
/kl setspawn red       - Redチームのスポーンを設定
/kl setcore blue       - Blueチームのコアを設定（見ている先のブロック）
/kl setcore red        - Redチームのコアを設定（見ている先のブロック）
/kl setnpc blue        - BlueチームのNPCを設定
/kl setnpc red         - RedチームのNPCを設定
/kl setarea B pos1     - Bエリアの角1を設定
/kl setarea B pos2     - Bエリアの角2を設定
/kl setlobby           - ロビーを設定
/kl save               - 設定を保存
```
