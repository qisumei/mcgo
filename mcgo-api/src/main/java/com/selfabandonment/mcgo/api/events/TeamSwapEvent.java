package com.selfabandonment.mcgo.api.events;

/**
 * Event fired when teams swap sides in a match.
 * 
 * <p>This event occurs typically at halftime when CT and T teams exchange sides.
 * 
 * <p><b>Note:</b> This is a platform-independent event interface. Platform-specific
 * implementations will need to provide the actual match and player data.
 * 
 * @since 1.1.5
 */
public interface TeamSwapEvent extends DomainEvent {
    
    /**
     * Gets the match ID where the team swap occurred.
     * 
     * @return the match identifier
     */
    String getMatchId();
    
    /**
     * Gets the current round number when the swap occurred.
     * 
     * @return the round number
     */
    int getCurrentRound();
}
