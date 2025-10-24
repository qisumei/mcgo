# MCGO API Module

## Purpose
This module contains the stable public API for MCGO:
- Interfaces and contracts
- DTOs (Data Transfer Objects)
- SPIs (Service Provider Interfaces)
- Constants and error codes
- Event definitions

## Principles
- **Zero Implementation**: Only interfaces and data classes
- **Platform Independent**: No NeoForge or Minecraft dependencies
- **Binary Compatibility**: Maintain API stability across versions
- **Minimal Dependencies**: Pure Java only

## Package Structure
```
com.selfabandonment.mcgo.api/
├── events/          # Domain event definitions
├── commands/        # Command interfaces
├── dto/             # Data transfer objects
├── spi/             # Service provider interfaces
└── errors/          # Error codes and exceptions
```

## Usage
This module is the foundation for all other modules. Any external integration should depend only on this API module to ensure loose coupling.

## Migration Status
🔄 **Phase 1 - Skeleton**: Module structure created, awaiting code migration
