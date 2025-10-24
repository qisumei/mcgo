package com.selfabandonment.mcgo.api.events;

/**
 * Listener interface for match-related events.
 * 
 * <p>Implementations of this interface can be registered with the event bus
 * to receive notifications about match events.
 * 
 * <p><b>Migration Note:</b> Platform-independent version of the original
 * {@code com.qisumei.csgo.events.match.MatchEventListener}.
 * 
 * @since 1.1.5
 */
public interface MatchEventListener {
    
    /**
     * Called when teams swap sides (typically at halftime).
     * 
     * @param event the team swap event containing match and player information
     */
    default void onTeamSwap(TeamSwapEvent event) {}
    
    /**
     * Called when a new round starts.
     * 
     * @param event the round start event
     */
    default void onRoundStart(RoundStartEvent event) {}
}
