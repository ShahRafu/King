// Removed duplicate Room Entity/DAO definitions.
// This file used to define MemoryEntity and MemoryDao in the old `memory` package.
// Those types have been consolidated under `com.shahrafuking.kingassistant.storage.room`.
// This placeholder file no longer contains Room annotations so there are no duplicate
// entities/DAOs during Room annotation processing. Delete this file when all callers
// have migrated to the storage.room package.

@Deprecated("Memory types have been moved to com.shahrafuking.kingassistant.storage.room")
class MemoryPackageMigrationPlaceholder
