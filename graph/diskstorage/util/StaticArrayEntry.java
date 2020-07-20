/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.core.graph.diskstorage.util;

import com.google.common.base.Preconditions;
import grakn.core.graph.diskstorage.Entry;
import grakn.core.graph.diskstorage.EntryMetaData;
import grakn.core.graph.diskstorage.MetaAnnotatable;
import grakn.core.graph.diskstorage.StaticBuffer;
import grakn.core.graph.graphdb.relations.RelationCache;

import java.nio.ByteBuffer;
import java.util.Map;


public class StaticArrayEntry extends BaseStaticArrayEntry implements Entry, MetaAnnotatable {

    public static final EntryMetaData[] EMPTY_SCHEMA = new EntryMetaData[0];
    public static final GetColVal<Entry, StaticBuffer> ENTRY_GETTER = new GetColVal<Entry, StaticBuffer>() {
        @Override
        public StaticBuffer getColumn(Entry entry) {
            return entry.getColumn();
        }

        @Override
        public StaticBuffer getValue(Entry entry) {
            return entry.getValue();
        }

        @Override
        public EntryMetaData[] getMetaSchema(Entry element) {
            if (!element.hasMetaData()) return EMPTY_SCHEMA;
            Map<EntryMetaData, Object> metas = element.getMetaData();
            return metas.keySet().toArray(new EntryMetaData[metas.size()]);
        }

        @Override
        public Object getMetaData(Entry element, EntryMetaData meta) {
            return element.getMetaData().get(meta);
        }


    };
    private Map<EntryMetaData, Object> metadata = EntryMetaData.EMPTY_METADATA;
    /**
     * ############# IDENTICAL CODE ###############
     */

    private volatile transient RelationCache cache;

    public StaticArrayEntry(byte[] array, int offset, int limit, int valuePosition) {
        super(array, offset, limit, valuePosition);
    }

    //########## META DATA ############

    public StaticArrayEntry(byte[] array, int limit, int valuePosition) {
        super(array, limit, valuePosition);
    }

    public StaticArrayEntry(byte[] array, int valuePosition) {
        super(array, valuePosition);
    }

    public StaticArrayEntry(StaticBuffer buffer, int valuePosition) {
        super(buffer, valuePosition);
    }

    StaticArrayEntry(Entry entry) {
        super(entry, entry.getValuePosition());
    }

    public static Entry of(StaticBuffer buffer) {
        return new StaticArrayEntry(buffer, buffer.length());
    }

    public static <E> Entry ofBytes(E element, StaticArrayEntry.GetColVal<E, byte[]> getter) {
        return of(element, getter, ByteArrayHandler.INSTANCE);
    }

    public static <E> Entry ofByteBuffer(E element, StaticArrayEntry.GetColVal<E, ByteBuffer> getter) {
        return of(element, getter, ByteBufferHandler.INSTANCE);
    }

    //########### CONSTRUCTORS AND UTILITIES ###########

    public static <E> Entry ofStaticBuffer(E element, StaticArrayEntry.GetColVal<E, StaticBuffer> getter) {
        return of(element, getter, StaticBufferHandler.INSTANCE);
    }

    public static <E> Entry of(StaticBuffer column, StaticBuffer value) {
        return of(column, value, StaticBufferHandler.INSTANCE);
    }

    private static <E, D> Entry of(E element, StaticArrayEntry.GetColVal<E, D> getter, StaticArrayEntry.DataHandler<D> dataHandler) {
        StaticArrayEntry entry = of(getter.getColumn(element), getter.getValue(element), dataHandler);
        //Add meta data if exists
        if (getter.getMetaSchema(element).length > 0) {
            for (EntryMetaData meta : getter.getMetaSchema(element)) {
                entry.setMetaData(meta, getter.getMetaData(element, meta));
            }
        }
        return entry;
    }

    private static <E, D> StaticArrayEntry of(D column, D value, StaticArrayEntry.DataHandler<D> dataHandler) {
        int valuePos = dataHandler.getSize(column);
        byte[] data = new byte[valuePos + dataHandler.getSize(value)];
        dataHandler.copy(column, data, 0);
        dataHandler.copy(value, data, valuePos);
        return new StaticArrayEntry(data, valuePos);
    }

    @Override
    public synchronized Object setMetaData(EntryMetaData key, Object value) {
        if (metadata == EntryMetaData.EMPTY_METADATA) metadata = new EntryMetaData.Map();
        return metadata.put(key, value);
    }

    @Override
    public boolean hasMetaData() {
        return !metadata.isEmpty();
    }

    @Override
    public Map<EntryMetaData, Object> getMetaData() {
        return metadata;
    }

    @Override
    public RelationCache getCache() {
        return cache;
    }

    @Override
    public void setCache(RelationCache cache) {
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    enum ByteArrayHandler implements DataHandler<byte[]> {

        INSTANCE;

        @Override
        public int getSize(byte[] data) {
            return data.length;
        }

        @Override
        public void copy(byte[] data, byte[] dest, int destOffset) {
            System.arraycopy(data, 0, dest, destOffset, data.length);
        }
    }

    enum ByteBufferHandler implements DataHandler<ByteBuffer> {

        INSTANCE;

        @Override
        public int getSize(ByteBuffer data) {
            return data.remaining();
        }

        @Override
        public void copy(ByteBuffer data, byte[] dest, int destOffset) {
            if (data.hasArray()) {
                System.arraycopy(data.array(), data.arrayOffset() + data.position(), dest, destOffset, data.remaining());
            } else {
                data.mark();
                data.get(dest, destOffset, data.remaining());
                data.reset();
            }
        }
    }


    enum StaticBufferHandler implements DataHandler<StaticBuffer> {

        INSTANCE;

        @Override
        public int getSize(StaticBuffer data) {
            return data.length();
        }

        @Override
        public void copy(StaticBuffer data, byte[] dest, int destOffset) {
            if (data instanceof StaticArrayBuffer) {
                StaticArrayBuffer buffer = (StaticArrayBuffer) data;
                buffer.copyTo(dest, destOffset);
            } else throw new IllegalArgumentException("Expected StaticArrayBuffer but got: " + data.getClass());
        }
    }

    public interface GetColVal<E, D> {

        D getColumn(E element);

        D getValue(E element);

        EntryMetaData[] getMetaSchema(E element);

        Object getMetaData(E element, EntryMetaData meta);

    }

    public interface DataHandler<D> {

        int getSize(D data);

        void copy(D data, byte[] dest, int destOffset);

    }

}
