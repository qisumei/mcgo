/**
 * MCGO Core Module - Platform-independent business logic.
 * 
 * <p>This module contains pure Java implementations of core game mechanics
 * without any platform-specific dependencies.
 * 
 * <p><b>Domain Areas:</b>
 * <ul>
 *   <li><b>domain/</b> - Domain entities and value objects (Match, Player, Item, etc.)</li>
 *   <li><b>services/</b> - Domain services implementing business logic</li>
 *   <li><b>policies/</b> - Business rules and policies</li>
 *   <li><b>usecase/</b> - Application use cases orchestrating domain logic</li>
 * </ul>
 * 
 * <p><b>Design Principles:</b>
 * <ul>
 *   <li>Platform independent - zero NeoForge/Minecraft dependencies</li>
 *   <li>Testable - 100% pure Java, easily unit tested</li>
 *   <li>Domain-driven - rich domain models with business logic</li>
 *   <li>Only depends on mcgo-api</li>
 * </ul>
 * 
 * <p><b>Key Components (to be migrated):</b>
 * <ul>
 *   <li>Match management and lifecycle</li>
 *   <li>Map and arena logic</li>
 *   <li>Item and weapon systems</li>
 *   <li>Game rules and validation</li>
 *   <li>Player matching algorithms</li>
 *   <li>Cooldown management</li>
 *   <li>Scoring system</li>
 * </ul>
 * 
 * <p><b>Testing:</b> This module should have the highest test coverage
 * as it contains pure business logic without external dependencies.
 * 
 * <p><b>Migration Status:</b> Phase 1 - Skeleton created, awaiting code migration from src/main/java.
 * 
 * @since 1.1.5
 */
package com.selfabandonment.mcgo.core;
