# Skill: android-room-timeseries

Efficient logging and querying of event data using Room Database.

## Pattern: Time-series Logging
Entities should always include a `Long` timestamp.

```kotlin
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

## Pattern: Async Inserts from Services
Services should use `Dispatchers.IO` for database operations to avoid blocking the main thread.

```kotlin
val scope = CoroutineScope(Dispatchers.IO)
scope.launch {
    database.dao().insert(event)
}
```

## Pattern: Aggregation Queries
Leverage SQL for grouping and counting directly in the DAO.

```kotlin
@Query("SELECT package, COUNT(*) as count FROM events WHERE timestamp > :since GROUP BY package")
fun getCountsSince(since: Long): List<PackageCount>
```

## Common Pitfalls
- **Main Thread DB Access**: Room throws an exception if you try to access the DB on the UI thread.
- **Migration Errors**: Always define a migration strategy or use `fallbackToDestructiveMigration()` during rapid prototyping.
- **Large Tables**: Implement a "Cleanup Task" to delete records older than 30 days to keep the app lightweight.
