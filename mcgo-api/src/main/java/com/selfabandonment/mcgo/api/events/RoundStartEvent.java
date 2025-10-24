package com.selfabandonment.mcgo.api.events;

/**
 * Event fired when a new round starts in a match.
 * 
 * <p>This event is triggered at the beginning of each round, providing
 * information about the round number and whether it's a pistol round.
 * 
 * @since 1.1.5
 */
public interface RoundStartEvent extends DomainEvent {
    
    /**
     * Gets the match ID where the round started.
     * 
     * @return the match identifier
     */
    String getMatchId();
    
    /**
     * Gets the round number that is starting.
     * 
     * @return the round number (1-based)
     */
    int getRoundNumber();
    
    /**
     * Checks if this is a pistol round (first round of each half).
     * 
     * @return true if this is a pistol round, false otherwise
     */
    boolean isPistolRound();
}
