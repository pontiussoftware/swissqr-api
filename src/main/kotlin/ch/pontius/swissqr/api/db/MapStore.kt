package ch.pontius.swissqr.api.db

import ch.pontius.swissqr.api.model.Entity
import ch.pontius.swissqr.api.model.Id
import ch.pontius.swissqr.api.utilities.optimisticRead
import ch.pontius.swissqr.api.utilities.read
import ch.pontius.swissqr.api.utilities.write
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.locks.StampedLock

/**
 * A simple [MapStore] implementation used for persisting [Entity] objects mapped to a given [Id].
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class MapStore<T: Entity>(path: Path, private val serializer: Serializer<T>): Iterable<T?>, AutoCloseable {

    init {
        Files.createDirectories(path.parent)
    }

    /** The [DB] object used to store */
    private val db = DBMaker.fileDB(path.toFile()).transactionEnable().fileMmapEnableIfSupported().make()

    /** Internal data structure used to keep track of the data held by this [MapStore]. */
    private val data = this.db.hashMap("data", Serializer.STRING, this.serializer).counterEnable().createOrOpen()

    /** Stamped lock to mediate read/write operations through this [MapStore]. */
    private val lock: StampedLock = StampedLock()

    /** Name of the entity accessed through this [MapStore]. */
    val name = path.fileName.toString().replace(".db","")

    /** Size of this [MapStore]. */
    val size: Int
        get() = this.data.size

    init {
        this.db.commit()
    }

    /**
     * Returns the value [T] for the given ID.
     *
     * @param id The ID of the entry.
     * @return Entry [T]
     */
    operator fun get(id: Id) = this.lock.optimisticRead { this.data[id.value] }

    /**
     * Returns true if value for given key exists and false otherwise.
     *
     * @param id The key of the entry.
     * @return Entry [T]
     */
    fun exists(id: Id) = this.lock.optimisticRead { this.data.containsKey(id.value) }

    /**
     * Deletes the value [T] for the given ID.
     *
     * @param id The ID of the entry that should be deleted
     * @return Deleted entry [T]
     */
    fun delete(id: Id): T? = this.lock.write {
        val deleted = this.data.remove(id.value)
        this.db.commit()
        return deleted
    }

    /**
     * Deletes the value [T]
     *
     * @param value The value that should be deleted.
     * @return Deleted entry [T]
     */
    fun delete(value: T) = this.delete(value.id)

    /**
     * Deletes all values with given ids
     */
    fun batchDelete(ids: Iterable<Id>) = this.lock.write {
        for (id in ids){
            val t = this.data[id.value]
            this.data.remove(id.value)
        }
        this.db.commit()
    }

    /**
     * Updates the value for the given ID with the new value [T]. If [T] doesn't exist yet, a new entry is created.
     *
     * @param value The new value [T]
     */
    fun update(value: T) = this.lock.write {
        this.data[value.id.value] = value
        this.db.commit()
    }

    /**
     * Appends the given values using this [MapStore]
     *
     * @param values An iterable of the values [T] that should be appended.
     */
    fun batchUpdate(values: Iterable<T>): List<Id> = this.lock.write {
        val ids = values.map {
            this.data[it.id.value] = it
            it.id
        }
        this.db.commit()
        ids
    }

    /**
     * Closes this [MapStore].
     */
    override fun close() {
        if (!this.db.isClosed()) {
            this.data.close()
            this.db.close()
        }
    }

    /**
     * Returns an [Iterator] for all entries in this [MapStore].
     */
    override fun iterator(): Iterator<T?> = object : Iterator<T?> {
        /** Internal list of keys. */
        val keys = this@MapStore.lock.read {
            LinkedList(this@MapStore.data.keys)
        }
        override fun hasNext(): Boolean = this.keys.isNotEmpty()
        override fun next(): T? = this@MapStore.lock.read {
            return this@MapStore[object: Id {
                override val value: String
                    get() = keys.poll()
            }]
        }
    }
}