# King's Line - アーキテクチャ設計書

## 概要

King's Lineは2チーム対戦型PvPミニゲームプラグインです。
Spigot 1.8.8向けに開発されています。

---

## パッケージ構造

```
tensaimc.kingsline/
├── KingsLine.java                 # メインプラグインクラス
│
├── game/                          # ゲーム管理
│   ├── GameManager.java           # ゲーム全体の制御
│   └── GameState.java             # ゲーム状態 (enum)
│
├── player/                        # プレイヤー管理
│   ├── KLPlayer.java              # プレイヤーデータ
│   ├── Team.java                  # チーム (enum)
│   ├── TeamManager.java           # チーム管理
│   └── PartyManager.java          # パーティー機能
│
├── arena/                         # アリーナ管理
│   ├── Arena.java                 # アリーナデータ
│   ├── AreaManager.java           # エリア占領管理
│   └── Area.java                  # エリア (A/B/C)
│
├── score/                         # スコア管理
│   └── ScoreManager.java          # ポイント管理
│
├── scoreboard/                    # UI表示
│   └── ScoreboardManager.java     # サイドバー、ボスバー管理
│
├── resource/                      # リソース管理
│   ├── ShardManager.java          # Shard管理
│   └── LuminaManager.java         # Lumina管理
│
├── element/                       # エレメントシステム
│   ├── Element.java               # エレメント種類 (enum)
│   └── ElementManager.java        # エレメント効果・SP技管理
│
├── king/                          # キングシステム
│   └── KingManager.java           # キング管理、オーラ効果
│
├── upgrade/                       # アップグレードシステム
│   ├── TeamUpgrade.java           # アップグレード種類 (enum)
│   └── UpgradeManager.java        # 投資・効果適用管理
│
├── npc/                           # NPC管理
│   └── NPCManager.java            # Villager NPC (ショップ/銀行)
│
├── gui/                           # GUI
│   ├── GUIManager.java            # GUI基盤
│   ├── ElementSelectGUI.java      # エレメント選択
│   ├── ShopGUI.java               # 個人ショップ
│   └── UpgradeGUI.java            # チームアップグレード
│
├── command/                       # コマンド
│   ├── KLCommand.java             # メインコマンド (/kl)
│   └── KLTabCompleter.java        # タブ補完
│
├── listener/                      # イベントリスナー
│   ├── PlayerListener.java        # プレイヤーイベント
│   ├── CombatListener.java        # 戦闘イベント
│   ├── GUIListener.java           # GUIクリックイベント
│   ├── NPCListener.java           # NPC操作イベント
│   ├── ItemListener.java          # アイテム拾得イベント
│   └── CoreListener.java          # コア破壊・警告イベント
│
├── config/                        # 設定管理
│   ├── ConfigManager.java         # config.yml 管理
│   └── ArenaConfig.java           # arenas.yml 管理
│
├── database/                      # データベース
│   └── StatsDatabase.java         # SQLite 統計管理
│
└── util/                          # ユーティリティ
    ├── TitleUtil.java             # Title送信 (1.8.8 NMS)
    ├── ActionBarUtil.java         # ActionBar送信 (1.8.8 NMS)
    └── BossBarManager.java        # BossBar (Wither方式)
```

---

## クラス設計

### コアクラス

#### KingsLine.java (メインクラス)
```java
public final class KingsLine extends JavaPlugin {
    private static KingsLine instance;
    
    // Config
    private ConfigManager configManager;
    private ArenaConfig arenaConfig;
    
    // Core Managers
    private GameManager gameManager;
    private TeamManager teamManager;
    private ScoreboardManager scoreboardManager;
    
    // Resource Managers
    private ShardManager shardManager;
    private LuminaManager luminaManager;
    
    // Feature Managers
    private ElementManager elementManager;
    private UpgradeManager upgradeManager;
    private NPCManager npcManager;
    private KingManager kingManager;
    
    // Listeners
    private CoreListener coreListener;
    
    // Database
    private StatsDatabase statsDatabase;
}
```

#### GameManager.java
```java
public class GameManager {
    private GameState state;           // WAITING, STARTING, RUNNING, ENDING
    private Arena currentArena;
    private Map<UUID, KLPlayer> players;
    
    // Score
    private int blueScore;
    private int redScore;
    private boolean blueCanRespawn;
    private boolean redCanRespawn;
    
    // Voting
    private Set<UUID> kingCandidatesBlue;
    private Set<UUID> kingCandidatesRed;
    private boolean votingPhase;
}
```

### プレイヤー関連

#### KLPlayer.java
```java
public class KLPlayer {
    private UUID uuid;
    private Team team;
    private Element element;
    
    // Shard（チームアップグレード用）
    private int shardCarrying;   // 所持中（未保護）
    private int shardSaved;      // 貯金済み（保護）
    
    // Lumina（個人ショップ用）
    private int luminaCarrying;  // 所持中（未保護）
    private int luminaSaved;     // 貯金済み（保護）
    
    // SP System
    private int spGauge;         // 0-10
    private long spCooldownEnd;
    
    // King
    private boolean isKing;
    
    // Game State
    private boolean canRespawn;
    private boolean isAlive;
    
    // Statistics
    private int killsThisGame;
    private int deathsThisGame;
}
```

---

## データフロー

### 通貨システム

```
[Shard拾得]
    ↓
KLPlayer.shardCarrying += amount
    ↓
[拠点に帰還（スポーン10ブロック以内）]
    ↓
KLPlayer.shardSaved += shardCarrying
KLPlayer.shardCarrying = 0
    ↓
[アップグレードGUIで投資]
    ↓
UpgradeManager.invest(klPlayer, upgrade, amount)
    ↓
[目標達成]
    ↓
チーム全員に効果適用
```

```
[死亡時]
    ↓
KLPlayer.shardCarrying → 地面にドロップ
KLPlayer.luminaCarrying → 地面にドロップ
    ↓
[敵が拾得]
    ↓
敵のcarryingに加算
```

### ゲームフロー

```
[WAITING]
    │ /kl start
    ▼
[STARTING]
    ├── チーム振り分け (PartyManager参照)
    ├── Title表示「⚔ KING'S LINE ⚔」
    ├── ネザースター配布 (エレメント選択)
    ├── !king で立候補受付
    └── キング選出
    │
    ▼
[RUNNING]
    ├── NPCスポーン
    ├── スコアボード開始
    ├── コア監視開始
    ├── Shardスポーン開始
    ├── エリア占領判定
    └── 500pt到達 → 相手リスポーン無効
    │
    ▼
[ENDING]
    ├── 勝者発表 (Title)
    ├── 統計保存 (SQLite)
    └── クリーンアップ
```

---

## 1.8.8 NMS対応

### Title/ActionBar
`PacketPlayOutTitle` と `PacketPlayOutChat` を使用してリフレクションで送信。

### BossBar
1.8.8にはBossBar APIがないため、Witherエンティティを使用したハック方式で実装。
- プレイヤーの視線の先（地下Y=-50）に見えないWitherをスポーン
- WitherのHPと名前を更新してバー表示

---

## 設定ファイル

| ファイル | 用途 |
|---------|------|
| `config.yml` | ゲーム設定（ポイント、クールダウン等） |
| `arenas.yml` | アリーナ設定（座標、エリア範囲） |
| `stats.db` | SQLiteデータベース（統計情報） |

---

## 依存関係

- **Spigot API 1.8.8**: 必須
- **SQLite JDBC**: Maven Shadeでバンドル
