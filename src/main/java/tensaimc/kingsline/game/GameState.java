package tensaimc.kingsline.game;

/**
 * ゲームの状態を表すenum
 */
public enum GameState {
    
    /**
     * ゲーム待機中 - プレイヤーが参加可能
     */
    WAITING("待機中"),
    
    /**
     * 開始準備中 - エレメント選択、キング投票
     */
    STARTING("準備中"),
    
    /**
     * ゲーム進行中 - PvP有効
     */
    RUNNING("進行中"),
    
    /**
     * 終了処理中 - 結果発表、クリーンアップ
     */
    ENDING("終了中");
    
    private final String displayName;
    
    GameState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
