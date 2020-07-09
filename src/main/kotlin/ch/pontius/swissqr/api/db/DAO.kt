package ch.pontius.swissqr.api.db

import ch.pontius.swissqr.api.model.Entity
import ch.pontius.swissqr.api.utilities.optimisticRead
import ch.pontius.swissqr.api.utilities.write
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.locks.StampedLock

/**
 * A simple data access object [DAO] implementation for the [Entity] objects used by Swiss QR code.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DAO<T: Entity>(path: Path, private val serializer: Serializer<T>) : Iterable<T>, AutoCloseable {

    init {
        Files.createDirectories(path.parent)
    }

    /** The [DB] object used to store */
    private val db = DBMaker.fileDB(path.toFile()).transactionEnable().fileMmapEnableIfSupported().make()

    /** Internal data structure used to keep track of the data held by this [DAO]. */
    private val data = this.db.hashMap("data", Serializer.STRING, this.serializer).counterEnable().createOrOpen()

    /** Stamped lock to mediate read/write operations through this [DAO]. */
    private val lock: StampedLock = StampedLock()

    /** Name of the entity accessed through this [DAO]. */
    val name = path.fileName.toString().replace(".db","")

    init {
        this.db.commit()
    }

    /**
     * Returns the value [T] for the given ID.
     *
     * @param id The ID of the entry.
     * @return Entry [T]
     */
    operator fun get(id: String) = this.lock.optimisticRead { this.data[id] }

    /**
     * Returns true if value for given key exists and false otherwise.
     *
     * @param id The key of the entry.
     * @return Entry [T]
     */
    fun exists(id: String) = this.lock.optimisticRead { this.data.containsKey(id) }

    /**
     * Deletes the value [T] for the given ID.
     *
     * @param id The ID of the entry that should be deleted
     * @return Deleted entry [T]
     */
    fun delete(id: String): T? = this.lock.write {
        val deleted = this.data.remove(id)
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
    fun batchDelete(ids: Iterable<String>) = this.lock.write {
        for (id in ids){
            val t = data[id]
            this.data.remove(id)
        }
        this.db.commit()
    }

    /**
     * Updates the value for the given ID with the new value [T]. If [T] doesn't exist yet, a new entry is created.
     *
     * @param value The new value [T]
     */
    fun update(value: T) = this.lock.write {
        this.data[value.id] = value
        this.db.commit()
    }

    /**
     * Appends the given values using this [DAO]
     *
     * @param values An iterable of the values [T] that should be appended.
     */
    fun batchAppend(values: Iterable<T>): List<String> = this.lock.write {
        val ids = values.map {
            this.data[it.id] = it
            it.id
        }
        this.db.commit()
        ids
    }

    /**
     * Closes this [DAO].
     */
    override fun close() {
        if (!this.db.isClosed()) {
            this.data.close()
            this.db.close()
        }
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        override fun hasNext(): Boolean {
            TODO("Not yet implemented")
        }

        override fun next(): T {
            TODO("Not yet implemented")
        }
    }
}