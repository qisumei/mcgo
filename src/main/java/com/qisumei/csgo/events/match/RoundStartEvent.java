package com.qisumei.csgo.events.match;

import com.qisumei.csgo.game.Match;

/**
 * 回合开始事件 - 在新回合开始时触发
 */
public class RoundStartEvent {
    private final Match match;
    private final int roundNumber;
    private final boolean isPistolRound;
    
    public RoundStartEvent(Match match, int roundNumber, boolean isPistolRound) {
        this.match = match;
        this.roundNumber = roundNumber;
        this.isPistolRound = isPistolRound;
    }
    
    public Match getMatch() {
        return match;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public boolean isPistolRound() {
        return isPistolRound;
    }
}
