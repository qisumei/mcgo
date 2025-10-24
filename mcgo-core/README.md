# MCGO Core Module

## Purpose
Pure Java domain implementation containing core business logic:
- Match management
- Map and arena logic
- Item and weapon systems
- Game rules and policies
- Player matching algorithms
- Cooldown management
- Scoring system

## Principles
- **Platform Independent**: Zero NeoForge/Minecraft dependencies
- **Testable**: 100% pure Java, easily unit tested
- **Domain-Driven**: Rich domain models and business logic
- **Dependency**: Only depends on mcgo-api

## Package Structure
```
com.selfabandonment.mcgo.core/
├── domain/          # Domain entities and value objects
├── services/        # Domain services
├── policies/        # Business rules and policies
└── usecase/         # Application use cases
```

## Testing
This module should have the highest test coverage as it contains pure business logic without external dependencies.

## Migration Status
🔄 **Phase 1 - Skeleton**: Module structure created, awaiting code migration from src/main/java
