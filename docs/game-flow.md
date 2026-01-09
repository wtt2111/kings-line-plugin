# King's Line - ゲームフロー

## 全体フロー

```
┌─────────────┐
│   WAITING   │  サーバー起動後の待機状態
└──────┬──────┘
       │ /kl start
       ▼
┌─────────────┐
│  STARTING   │  準備フェーズ (30-60秒)
│             │  - チーム振り分け
│             │  - エレメント選択
│             │  - キング投票
└──────┬──────┘
       │ 準備完了
       ▼
┌─────────────┐
│   RUNNING   │  本戦
│             │  - PvP有効
│             │  - エリア制圧
│             │  - Shard/Lumina収集
└──────┬──────┘
       │ 500pt到達 → 全滅
       ▼
┌─────────────┐
│   ENDING    │  終了処理
│             │  - 勝者発表
│             │  - 統計表示
│             │  - ワールドリセット
└─────────────┘
```

---

## Phase 1: WAITING (待機)

### 状態
- ゲームが開始されていない状態
- プレイヤーはサーバーに自由に出入り可能

### トリガー
- `/kl start` コマンドで STARTING へ遷移

---

## Phase 2: STARTING (準備フェーズ)

### 時間
- 30〜60秒 (config.ymlで設定可能)

### 処理順序

#### 1. チーム振り分け
```
1. パーティーをグループ化
2. パーティーごとにランダムでチームを割り当て
3. パーティーに属さないプレイヤーをバランス調整しながら割り当て
```

#### 2. エレメント選択
- 全プレイヤーに「エレメント選択アイテム」を配布
- 右クリックでGUIを開き、Fire/Ice/Wind/Earthから選択
- 時間内に選択しない場合はランダム

#### 3. キング投票
```
1. 立候補受付 (15秒)
   - チャットで「!king」と発言で立候補
   
2. 投票 (15秒)
   - 立候補者がいればGUIで投票
   - 最多票のプレイヤーがキングに決定
   
3. フォールバック
   - 立候補者0人 → ランダム or 最高スタッツ
   - 同票 → ランダム
```

#### 4. 初期装備配布
- 皮フルセット
- 石の剣
- 食料 (ステーキ16個など)
- エレメント選択アイテムは回収

#### 5. テレポート
- 各チームのスポーン地点へテレポート
- バリア状態 (移動不可 or 透明壁)

---

## Phase 3: RUNNING (本戦)

### 開始時
- バリア解除
- 全プレイヤーに開始メッセージ
- BGM再生 (オプション)

### 継続処理 (ゲームループ)

#### 3秒ごと: エリア占領判定
```java
// Bエリア占領ポイント
int blueCount = areaB.getPlayersInside(Team.BLUE).size();
int redCount = areaB.getPlayersInside(Team.RED).size();

if (blueCount > redCount) {
    scoreManager.addPoints(Team.BLUE, 2);
} else if (redCount > blueCount) {
    scoreManager.addPoints(Team.RED, 2);
}
// 同数は何もなし
```

#### 10秒ごと: Shardスポーン
```java
// A, B, Cエリアの指定座標にShardアイテムをドロップ
shardManager.spawnShard(areaA.getSpawnPoint());
shardManager.spawnShard(areaB.getSpawnPoint());
shardManager.spawnShard(areaC.getSpawnPoint());
```

#### 1秒ごと: キングオーラ
```java
// キングの周囲8ブロックの味方にSpeed I
for (KLPlayer klp : getTeamPlayers(king.getTeam())) {
    if (klp.getLocation().distance(king.getLocation()) <= 8) {
        klp.addPotionEffect(new PotionEffect(SPEED, 40, 0));
    }
}
```

### イベント処理

#### キル発生時
```java
// ポイント加算
if (victim.isKing()) {
    scoreManager.addPoints(killer.getTeam(), 20);      // キングキル
    scoreManager.addPoints(victim.getTeam(), -50);     // ペナルティ
    // 敵全員にStrength 15秒
    // Shard 5個ドロップ
} else {
    scoreManager.addPoints(killer.getTeam(), 4);       // 通常キル
}

// Lumina加算
luminaManager.addLumina(killer, 2);

// SPゲージ (キラーが与えたヒット数でカウント済み)
```

#### Shard拾得時
```java
// インベントリに追加 (未確定状態)
klPlayer.addShardCarrying(1);
player.sendMessage("Shard を拾いました！拠点に持ち帰ってください。");
```

#### Shard銀行 (NPC右クリック)
```java
int carrying = klPlayer.getShardCarrying();
if (carrying > 0) {
    teamManager.addTeamShard(klPlayer.getTeam(), carrying);
    klPlayer.setShardCarrying(0);
    player.sendMessage(carrying + " Shard をチームに納めました！");
}
```

#### 死亡時
```java
// 所持Shardをドロップ
if (klPlayer.getShardCarrying() > 0) {
    shardManager.dropShard(deathLocation, klPlayer.getShardCarrying());
    klPlayer.setShardCarrying(0);
}

// リスポーン可能か確認
if (klPlayer.canRespawn()) {
    // 5秒後にリスポーン
    scheduleRespawn(klPlayer, 5);
} else {
    // 観戦モードへ
    setSpectator(klPlayer);
}
```

### 500pt到達時
```java
Team winner = scoreManager.getLeadingTeam();
Team loser = winner.getOpposite();

// 敗北チームのリスポーン無効化
for (KLPlayer klp : getTeamPlayers(loser)) {
    klp.setCanRespawn(false);
}

broadcast("§c" + loser.getName() + "チームのリスポーンが無効化されました！");
```

---

## Phase 4: ENDING (終了)

### トリガー
- リスポーン無効化後、片方のチームが全滅

### 処理

#### 1. ゲーム停止
- PvP無効化
- 全プレイヤーを動けなくする (フリーズ)

#### 2. 結果発表 (5秒間)
```
=====================================
        🏆 BLUE TEAM WINS! 🏆
=====================================
  Final Score: BLUE 523 - RED 500
  
  MVP: PlayerName (12 Kills)
  
  King Kills: 2
  Core Destroyed: Yes
=====================================
```

#### 3. 統計保存
```java
for (KLPlayer klp : getAllPlayers()) {
    database.addKills(klp.getUuid(), klp.getKillsThisGame());
    database.addDeaths(klp.getUuid(), klp.getDeathsThisGame());
    if (klp.getTeam() == winnerTeam) {
        database.addWin(klp.getUuid());
    }
}
```

#### 4. クリーンアップ
- 全プレイヤーをロビーへテレポート (設定されていれば)
- プレイヤーデータリセット
- アリーナリセット (オプション)
- GameState → WAITING

---

## スケール切り替え

### 判定タイミング
- STARTING フェーズ開始時

### ルール
```java
int totalPlayers = getPlayerCount();
int perTeam = totalPlayers / 2;

if (perTeam <= 4) {
    // 小規模モード: Bエリアのみ
    areaA.setEnabled(false);
    areaC.setEnabled(false);
} else {
    // 中〜大規模モード: A/B/C全て
    areaA.setEnabled(true);
    areaC.setEnabled(true);
}
```
