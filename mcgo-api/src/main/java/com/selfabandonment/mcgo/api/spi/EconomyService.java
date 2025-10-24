package com.selfabandonment.mcgo.api.spi;

/**
 * Service Provider Interface for economy operations.
 * 
 * <p>This interface defines the contract for managing player currency
 * in a platform-independent way. Platform-specific implementations
 * will handle the actual player references.
 * 
 * <p><b>Migration Note:</b> Platform-independent version of
 * {@code com.qisumei.csgo.service.EconomyService}.
 * 
 * @since 1.1.5
 */
public interface EconomyService {
    
    /**
     * Adds money to a player's account.
     * 
     * @param playerId the unique identifier of the player
     * @param amount the amount to add
     */
    void giveMoney(String playerId, int amount);
    
    /**
     * Sets a player's money to a specific amount.
     * 
     * @param playerId the unique identifier of the player
     * @param amount the new money amount
     */
    void setMoney(String playerId, int amount);
    
    /**
     * Gets the current money amount for a player.
     * 
     * @param playerId the unique identifier of the player
     * @return the current money amount
     */
    int getMoney(String playerId);
    
    /**
     * Gets the reward amount for a kill with a specific weapon.
     * 
     * @param weaponId the weapon item identifier (e.g., "pointblank:ak47")
     * @return the reward amount
     */
    int getRewardForKill(String weaponId);
    
    /**
     * Checks if a player can afford to purchase an item.
     * 
     * @param playerId the unique identifier of the player
     * @param itemId the item identifier
     * @return true if the player can afford the item
     */
    default boolean canAfford(String playerId, String itemId) {
        int price = getPrice(itemId);
        return getMoney(playerId) >= price;
    }
    
    /**
     * Gets the price of an item.
     * 
     * @param itemId the item identifier
     * @return the price, or 0 if not defined
     */
    int getPrice(String itemId);
}
