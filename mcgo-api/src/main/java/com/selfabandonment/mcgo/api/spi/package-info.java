/**
 * Service Provider Interfaces for MCGO.
 * 
 * <p>This package contains all service interfaces that define extension points
 * for the MCGO platform. Implementations should be provided by concrete modules.
 * 
 * <p><b>Key Interfaces:</b>
 * <ul>
 *   <li>EconomyService - Economy and currency management</li>
 *   <li>MatchService - Match lifecycle and state management</li>
 *   <li>InventoryGateway - Inventory operations</li>
 *   <li>PermissionGateway - Permission and authorization</li>
 *   <li>MessageBus - Event and message distribution</li>
 *   <li>ConfigService - Configuration management</li>
 * </ul>
 * 
 * <p>These interfaces follow the Ports and Adapters pattern, where:
 * <ul>
 *   <li><b>Port</b>: The interface defined here</li>
 *   <li><b>Adapter</b>: Implementation in concrete modules (core, platform, etc.)</li>
 * </ul>
 * 
 * @since 1.1.5
 */
package com.selfabandonment.mcgo.api.spi;
