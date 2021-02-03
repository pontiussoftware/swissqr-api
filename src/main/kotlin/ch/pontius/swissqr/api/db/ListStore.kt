package ch.pontius.swissqr.api.db

import ch.pontius.swissqr.api.model.Entity
import ch.pontius.swissqr.api.utilities.read
import ch.pontius.swissqr.api.utilities.write
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Path
import java.util.concurrent.locks.StampedLock

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ListStore<T>(path: Path, private val serializer: Serializer<T>) : MutableIterable<T?>, AutoCloseable {
    /** The [DB] object used to store */
    private val db = DBMaker.fileDB(path.toFile()).transactionEnable().make()

    /** Internal data structure used to keep track of the data held by this [DAO]. */
    private val data = this.db.indexTreeList("data", this.serializer).createOrOpen()

    /** Stamped lock to mediate read/write operations through this [DAO]. */
    private val lock: StampedLock = StampedLock()

    /** Name of the entity accessed through this [DAO]. */
    val name = path.fileName.toString().replace(".db", "")

    /**
     * Returns the size of this [ListStore]
     *
     * @return Size of this [ListStore]
     */
    fun size(): Int = this.lock.read {
        this.data.size
    }

    /**
     * Gets the item from this [ListStore] the given index.
     *
     * @param index The index to access the item at.
     * @return Item [T] or null
     */
    fun get(index: Int): T? = this.lock.read {
        this.data [index]
    }

    /**
     * Appends an item to this [ListStore].
     *
     * @param item The data item [T] to append.
     * @return true on success, false otherwise.
     */
    fun append(item: T?): Boolean= this.lock.write {
        this.data.add(item)
    }

    /**
     * Closes this [DAO].
     */
    override fun close() {
        if (!this.db.isClosed()) {
            this.db.close()
        }
    }

    /**
     * Returns an [MutableIterator] over the elements of this [ListStore].
     */
    override fun iterator(): MutableIterator<T?> = this.data.iterator()
}