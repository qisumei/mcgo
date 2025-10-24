package com.selfabandonment.mcgo.api.spi;

import java.util.Collection;
import java.util.Optional;

/**
 * Service Provider Interface for match management operations.
 * 
 * <p>This interface defines the contract for creating and managing
 * CSGO-style matches in a platform-independent way.
 * 
 * <p><b>Migration Note:</b> Platform-independent version of
 * {@code com.qisumei.csgo.service.MatchService}.
 * 
 * @since 1.1.5
 */
public interface MatchService {
    
    /**
     * Creates a new match with the specified parameters.
     * 
     * @param matchName the unique name of the match
     * @param maxPlayers the maximum number of players allowed
     * @return true if the match was created successfully
     */
    boolean createMatch(String matchName, int maxPlayers);
    
    /**
     * Gets a match by its name.
     * 
     * @param matchName the name of the match
     * @return an Optional containing the match if found
     */
    Optional<MatchInfo> getMatch(String matchName);
    
    /**
     * Gets all active matches.
     * 
     * @return a collection of all active matches
     */
    Collection<MatchInfo> getAllMatches();
    
    /**
     * Gets the match that a player is currently in.
     * 
     * @param playerId the unique identifier of the player
     * @return an Optional containing the match if the player is in one
     */
    Optional<MatchInfo> getPlayerMatch(String playerId);
    
    /**
     * Removes a match by its name.
     * 
     * @param matchName the name of the match to remove
     */
    void removeMatch(String matchName);
    
    /**
     * Performs periodic tick operations for all matches.
     * 
     * <p>This method should be called regularly (typically every game tick)
     * to update match state, timers, and other time-dependent operations.
     */
    void tick();
    
    /**
     * Minimal match information interface for platform-independent access.
     */
    interface MatchInfo {
        /**
         * Gets the match name.
         * 
         * @return the match name
         */
        String getName();
        
        /**
         * Gets the current number of players in the match.
         * 
         * @return the player count
         */
        int getPlayerCount();
        
        /**
         * Gets the maximum number of players allowed.
         * 
         * @return the max players
         */
        int getMaxPlayers();
        
        /**
         * Checks if the match is currently active.
         * 
         * @return true if the match is active
         */
        boolean isActive();
    }
}
