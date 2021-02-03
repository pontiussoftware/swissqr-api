package ch.pontius.swissqr.api.db

import ch.pontius.swissqr.api.model.Id
import ch.pontius.swissqr.api.utilities.read
import ch.pontius.swissqr.api.utilities.write
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Path
import java.util.concurrent.locks.StampedLock

/**
 * A simple [ListStore] implementation used persisting objects to a growing list.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ListStore<T>(path: Path, private val serializer: Serializer<T>) : MutableIterable<T?>, AutoCloseable {
    /** The [DB] object used to store */
    private val db = DBMaker.fileDB(path.toFile()).transactionEnable().make()

    /** Internal data structure used to keep track of the data held by this [MapStore]. */
    private val data = this.db.indexTreeList("data", this.serializer).createOrOpen()

    /** Stamped lock to mediate read/write operations through this [MapStore]. */
    private val lock: StampedLock = StampedLock()

    /** Name of the entity accessed through this [MapStore]. */
    val name = path.fileName.toString().replace(".db", "")

    init {
        this.db.commit()
    }

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
        val ret = this.data.add(item)
        this.db.commit()
        ret
    }

    /**
     * Appends the given values using this [ListStore]
     *
     * @param values An iterable of the values [T] that should be appended.
     */
    fun batchAppend(values: Iterable<T?>): Boolean = this.lock.write {
        val success = values.all { this.data.add(it) }
        if (success) {
            this.db.commit()
        } else {
            this.db.rollback()
        }
        success
    }

    /**
     * Closes this [MapStore].
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