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
  
  # キングアシスト
  king-assist: 5
  
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
  # 投票フェーズ: 立候補受付時間 (秒)
  candidacy-duration: 15
  
  # 投票フェーズ: 投票時間 (秒)
  vote-duration: 15
  
  # オーラの範囲 (ブロック)
  aura-radius: 8
  
  # キング死亡時の敵バフ時間 (秒)
  death-buff-duration: 15

# エレメント設定
element:
  # エレメント選択の制限時間 (秒)
  selection-duration: 20
  
  # SP技のクールダウン (秒)
  sp-cooldown: 15
  
  # SP技に必要なヒット数
  sp-required-hits: 10
  
  # Fire
  fire:
    damage-bonus: 0.07        # +7%
    damage-taken-bonus: 0.05  # +5% (デメリット)
    burn-chance: 0.10         # 10%
    burn-duration: 20         # 1秒 (tick)
    # Overheat (SP技)
    overheat-duration: 100    # 5秒 (tick)
    overheat-damage-bonus: 0.20
    overheat-burn-duration: 40
    overheat-damage-taken: 0.10
  
  # Ice
  ice:
    speed-penalty: 0.05       # -5%
    knockback-resist: 0.20    # -20% (エリア内)
    slow-chance: 0.20         # 20%
    slow-duration: 20         # 1秒 (tick)
    # Ice Age (SP技)
    ice-age-radius: 6
    ice-age-max-targets: 2
    ice-age-freeze-duration: 30  # 1.5秒 (tick)
  
  # Wind
  wind:
    damage-taken-bonus: 0.10  # +10% (デメリット)
    # 常時Speed I, Jump Boost I
    # Gale Step (SP技)
    gale-step-range: 8
    gale-step-speed-duration: 80  # 4秒 (tick)
    gale-step-knockback-bonus: 0.30
  
  # Earth
  earth:
    damage-resist: 0.10       # -10%
    knockback-resist: 0.30    # -30%
    speed-penalty: 0.05       # -5%
    # Bulwark (SP技)
    bulwark-duration: 100     # 5秒 (tick)
    bulwark-damage-resist: 0.20
    bulwark-speed-penalty: 0.20

# アップグレード設定
upgrade:
  # 攻撃ライン
  attack:
    level1:
      cost: 30
      effect: "sharpness:1"
    level2:
      cost: 60
      effect: "sharpness:2"
    level3:
      cost: 90
      effect: "damage_bonus:0.05"
  
  # 防御ライン
  defense:
    level1:
      cost: 30
      effect: "protection:1"
    level2:
      cost: 60
      effect: "protection:2"
    level3:
      cost: 90
      effect: "damage_resist:0.05"
  
  # HPライン
  health:
    level1:
      cost: 40
      effect: "health_boost:1"
    level2:
      cost: 80
      effect: "health_boost:2"

# ショップ設定 (Lumina消費)
shop:
  items:
    speed_potion:
      cost: 5
      material: POTION
      potion-type: SPEED
      display-name: "&bスピードポーション"
    
    regen_potion:
      cost: 8
      material: POTION
      potion-type: REGEN
      display-name: "&d再生のポーション"
    
    ender_pearl:
      cost: 10
      material: ENDER_PEARL
      amount: 1
      display-name: "&5エンダーパール"
    
    golden_apple:
      cost: 15
      material: GOLDEN_APPLE
      amount: 1
      display-name: "&6金のリンゴ"

# 初期装備
starting-gear:
  helmet:
    material: LEATHER_HELMET
  chestplate:
    material: LEATHER_CHESTPLATE
  leggings:
    material: LEATHER_LEGGINGS
  boots:
    material: LEATHER_BOOTS
  weapon:
    material: STONE_SWORD
  food:
    material: COOKED_BEEF
    amount: 16

# データベース
database:
  # SQLiteファイル名
  filename: "stats.db"
```

---

## arenas.yml

アリーナ (マップ) の設定を管理します。

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
    world: "kingsline_castle"
    
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
    
    # ロビー位置 (ゲーム終了後のテレポート先、オプション)
    lobby:
      x: 0.5
      y: 65.0
      z: 0.5
      yaw: 0.0
      pitch: 0.0

  # 別のアリーナ例
  desert:
    world: "kingsline_desert"
    spawns:
      blue:
        x: 0.5
        y: 70.0
        z: -100.5
      red:
        x: 0.5
        y: 70.0
        z: 100.5
    # ... 以下同様
```

---

## messages.yml

メッセージを管理します（将来の多言語対応用）。

```yaml
# ========================================
# King's Line - Messages
# ========================================

prefix: "&8[&6KingsLine&8] "

game:
  starting: "&aゲームが開始されます！準備してください。"
  started: "&a&lゲーム開始！"
  ending: "&e{team} &aチームの勝利！"
  
team:
  assigned: "&a{team} &fチームに配属されました。"
  
element:
  select-prompt: "&eエレメントを選択してください！アイテムを右クリック！"
  selected: "&a{element} &fを選択しました！"
  random: "&7エレメントが自動的に &a{element} &7に設定されました。"

king:
  candidacy-start: "&eキング立候補を受け付けています！チャットで &6!king &eと発言！"
  candidacy-registered: "&aキングに立候補しました！"
  vote-start: "&eキング投票を開始します！GUIで投票してください。"
  elected: "&6{player} &eが &6キング &eに選出されました！"
  killed: "&c&l{team}のキングが倒されました！"

score:
  kill: "&a+{points}pt &7(キル)"
  king-kill: "&6+{points}pt &7(キングキル)"
  area-capture: "&b+{points}pt &7(エリア制圧)"
  core-destroy: "&d+{points}pt &7(コア破壊)"
  respawn-disabled: "&c&l{team}チームのリスポーンが無効化されました！"

shard:
  picked-up: "&aShard x{amount} &fを拾いました！"
  deposited: "&a{amount} Shard &fをチームに納めました！"
  dropped: "&c所持していたShardをドロップしました。"

lumina:
  earned: "&e+{amount} Lumina"

shop:
  purchased: "&a{item} &fを購入しました！"
  not-enough: "&cLuminaが足りません。(必要: {cost})"

upgrade:
  purchased: "&aチームアップグレード: &e{upgrade} Lv{level}"
  not-enough: "&cShardが足りません。(必要: {cost})"
```

---

## コマンドでの設定

アリーナ設定はコマンドでも行えます：

```
/kl setspawn blue      - 現在位置をBlueチームのスポーンに設定
/kl setspawn red       - 現在位置をRedチームのスポーンに設定
/kl setcore blue       - 現在位置をBlueチームのコアに設定
/kl setcore red        - 現在位置をRedチームのコアに設定
/kl setnpc blue        - 現在位置をBlueチームのNPCに設定
/kl setnpc red         - 現在位置をRedチームのNPCに設定
/kl setarea B pos1     - 現在位置をBエリアの角1に設定
/kl setarea B pos2     - 現在位置をBエリアの角2に設定
/kl save               - 設定をarenas.ymlに保存
```
