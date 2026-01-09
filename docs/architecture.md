# King's Line - アーキテクチャ設計書

## 概要

King's Lineは2チーム対戦型PvPミニゲームプラグインです。  
Spigot 1.8.8向けに開発されます。

---

## パッケージ構造

```
tensaimc.kingsline/
├── KingsLine.java                 # メインプラグインクラス
│
├── game/                          # ゲーム管理
│   ├── GameManager.java           # ゲーム全体の制御
│   ├── GameState.java             # ゲーム状態 (enum)
│   └── GameSettings.java          # ゲームごとの設定値
│
├── player/                        # プレイヤー管理
│   ├── KLPlayer.java              # プレイヤーデータ
│   ├── TeamManager.java           # チーム管理 (Blue/Red)
│   ├── PartyManager.java          # パーティー機能
│   └── PlayerStats.java           # 統計データ (Kill/Death/Win)
│
├── arena/                         # アリーナ管理
│   ├── Arena.java                 # アリーナデータ
│   ├── ArenaManager.java          # アリーナ管理
│   └── Area.java                  # エリア (A/B/C)
│
├── score/                         # スコア管理
│   └── ScoreManager.java          # ポイント管理、勝利判定
│
├── resource/                      # リソース管理
│   ├── ShardManager.java          # Shard (チーム通貨)
│   └── LuminaManager.java         # Lumina (個人通貨)
│
├── element/                       # エレメントシステム
│   ├── Element.java               # エレメント種類 (enum)
│   ├── ElementManager.java        # エレメント管理
│   └── ability/                   # 各エレメントの能力
│       ├── FireAbility.java
│       ├── IceAbility.java
│       ├── WindAbility.java
│       └── EarthAbility.java
│
├── king/                          # キングシステム
│   ├── KingManager.java           # キング管理、オーラ効果
│   └── KingVoteManager.java       # キング投票
│
├── upgrade/                       # アップグレードシステム
│   ├── UpgradeManager.java        # チームアップグレード管理
│   ├── UpgradeType.java           # アップグレード種類 (enum)
│   └── UpgradeLine.java           # 攻撃/防御/HPライン
│
├── shop/                          # ショップシステム
│   ├── ShopManager.java           # ショップ管理
│   └── ShopItem.java              # 購入可能アイテム
│
├── npc/                           # NPC管理
│   └── NPCManager.java            # Villager NPC (ショップ/銀行)
│
├── gui/                           # GUI
│   ├── GUIManager.java            # GUI基盤
│   ├── ElementSelectGUI.java      # エレメント選択
│   ├── ShopGUI.java               # ショップ
│   ├── UpgradeGUI.java            # アップグレード
│   └── KingVoteGUI.java           # キング投票
│
├── command/                       # コマンド
│   ├── KLCommand.java             # メインコマンド (/kl)
│   └── KLTabCompleter.java        # タブ補完
│
├── listener/                      # イベントリスナー
│   ├── GameListener.java          # ゲームイベント
│   ├── PlayerListener.java        # プレイヤーイベント
│   ├── CombatListener.java        # 戦闘イベント
│   └── GUIListener.java           # GUIクリックイベント
│
├── config/                        # 設定管理
│   ├── ConfigManager.java         # config.yml 管理
│   ├── ArenaConfig.java           # arenas.yml 管理
│   └── MessageConfig.java         # messages.yml 管理
│
├── database/                      # データベース
│   └── StatsDatabase.java         # SQLite 統計管理
│
└── util/                          # ユーティリティ
    ├── ColorUtil.java             # カラーコード
    └── LocationUtil.java          # 座標ユーティリティ
```

---

## クラス設計

### コアクラス

#### KingsLine.java (メインクラス)
```java
public final class KingsLine extends JavaPlugin {
    private static KingsLine instance;
    private GameManager gameManager;
    private ConfigManager configManager;
    private ArenaConfig arenaConfig;
    private StatsDatabase database;
    // ... 各種マネージャー
}
```

#### GameManager.java
```java
public class GameManager {
    private GameState state;           // WAITING, STARTING, RUNNING, ENDING
    private Arena currentArena;
    private Map<UUID, KLPlayer> players;
    private TeamManager teamManager;
    private ScoreManager scoreManager;
    // ... ゲームループ管理
}
```

#### GameState.java
```java
public enum GameState {
    WAITING,    // ゲーム待機中
    STARTING,   // 開始準備中 (エレメント選択、キング投票)
    RUNNING,    // ゲーム進行中
    ENDING      // 終了処理中
}
```

### プレイヤー関連

#### KLPlayer.java
```java
public class KLPlayer {
    private UUID uuid;
    private Team team;                 // BLUE, RED, NONE
    private Element element;           // FIRE, ICE, WIND, EARTH
    private int shardCarrying;         // 所持中のShard (未確定)
    private int lumina;                // 個人通貨
    private int spGauge;               // SP技ゲージ (0-10)
    private boolean isKing;
    private boolean canRespawn;        // リスポーン可能か
    // ... getter/setter
}
```

#### Team.java
```java
public enum Team {
    BLUE("Blue", ChatColor.BLUE),
    RED("Red", ChatColor.RED),
    NONE("None", ChatColor.GRAY);
    
    private String displayName;
    private ChatColor color;
}
```

### エリア関連

#### Arena.java
```java
public class Arena {
    private String name;
    private String worldName;
    private Location blueSpawn;
    private Location redSpawn;
    private Location blueCore;
    private Location redCore;
    private Location blueNPC;          // ショップ/銀行NPC位置
    private Location redNPC;
    private Area areaB;                // 中央エリア (常に存在)
    private Area areaA;                // 自陣エリア (5v5以上)
    private Area areaC;                // 敵陣エリア (5v5以上)
}
```

#### Area.java
```java
public class Area {
    private String id;                 // "A", "B", "C"
    private Location pos1;             // 範囲の角1
    private Location pos2;             // 範囲の角2
    
    public boolean contains(Location loc);
    public List<Player> getPlayersInside();
}
```

---

## データフロー

```
[プレイヤー参加]
    ↓
GameManager.addPlayer()
    ↓
KLPlayer 作成
    ↓
TeamManager.assignTeam() ← PartyManager参照
    ↓
[ゲーム開始]
    ↓
ElementSelectGUI → KLPlayer.setElement()
    ↓
KingVoteManager → KingManager.setKing()
    ↓
[本戦]
    ↓
CombatListener → ScoreManager.addKillPoints()
                → LuminaManager.addLumina()
                → ElementManager.addSPGauge()
    ↓
AreaManager (3秒tick) → ScoreManager.addAreaPoints()
    ↓
ShardManager → KLPlayer.addShardCarrying()
             → (銀行で) TeamManager.addTeamShard()
    ↓
[500pt到達]
    ↓
ScoreManager → 相手チームのcanRespawn = false
    ↓
[全滅]
    ↓
GameManager.endGame()
    ↓
StatsDatabase.saveStats()
```

---

## 設定ファイル

| ファイル | 用途 |
|---------|------|
| `config.yml` | ゲーム設定 (ポイント、クールダウン、ダメージ倍率など) |
| `arenas.yml` | アリーナ設定 (座標、エリア範囲) |
| `messages.yml` | メッセージ (将来の多言語対応用) |

---

## 依存関係

- **Spigot API 1.8.8**: 必須
- **SQLite**: Java標準ライブラリ (JDBC) を使用、外部依存なし
