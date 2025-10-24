package com.selfabandonment.mcgo.api.events;

/**
 * Base interface for all domain events in MCGO.
 * 
 * <p>Domain events represent significant occurrences within the game domain
 * that other parts of the system may want to react to.
 * 
 * <p>This is a marker interface following the Domain-Driven Design pattern.
 * 
 * @since 1.1.5
 */
public interface DomainEvent {
    /**
     * Gets the timestamp when this event occurred.
     * 
     * @return the event timestamp in milliseconds since epoch
     */
    default long getTimestamp() {
        return System.currentTimeMillis();
    }
}
