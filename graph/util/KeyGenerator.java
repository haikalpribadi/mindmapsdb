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
 *
 */

package grakn.core.graph.util;

import grakn.core.common.iterator.ResourceIterator;
import grakn.core.graph.iid.PrefixIID;
import grakn.core.graph.iid.VertexIID;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static grakn.core.common.collection.Bytes.join;
import static grakn.core.common.collection.Bytes.longToSortedBytes;
import static grakn.core.common.collection.Bytes.shortToSortedBytes;
import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.copyOfRange;

public class KeyGenerator {

    public abstract static class Schema {

        protected AtomicInteger ruleKey;
        protected final ConcurrentMap<PrefixIID, AtomicInteger> typeKeys;
        protected final int initialValue;
        protected final int delta;

        Schema(final int initialValue, final int delta) {
            typeKeys = new ConcurrentHashMap<>();
            ruleKey = new AtomicInteger(initialValue);
            this.initialValue = initialValue;
            this.delta = delta;
        }

        public byte[] forType(final PrefixIID root) {
            return shortToSortedBytes(typeKeys.computeIfAbsent(
                    root, k -> new AtomicInteger(initialValue)
            ).getAndAdd(delta));
        }

        public byte[] forRule() {
            return shortToSortedBytes(ruleKey.getAndAdd(delta));
        }

        public static class Buffered extends Schema {

            public Buffered() {
                super(Encoding.Key.BUFFERED.initialValue(), Encoding.Key.BUFFERED.isIncrement() ? 1 : -1);
            }
        }

        public static class Persisted extends Schema {

            public Persisted() {
                super(Encoding.Key.PERSISTED.initialValue(), Encoding.Key.PERSISTED.isIncrement() ? 1 : -1);
            }

            public void sync(final Storage storage) {
                syncTypeKeys(storage);
                syncRuleKey(storage);
            }

            private void syncTypeKeys(final Storage storage) {
                for (Encoding.Vertex.Type encoding : Encoding.Vertex.Type.values()) {
                    final byte[] prefix = encoding.prefix().bytes();
                    final byte[] lastIID = storage.getLastKey(prefix);
                    final AtomicInteger nextValue = lastIID != null ?
                            new AtomicInteger(wrap(copyOfRange(lastIID, PrefixIID.LENGTH, VertexIID.Type.LENGTH)).getShort() + delta) :
                            new AtomicInteger(initialValue);
                    typeKeys.put(PrefixIID.of(encoding), nextValue);
                }
            }

            private void syncRuleKey(final Storage storage) {
                final byte[] prefix = Encoding.Vertex.Rule.RULE.prefix().bytes();
                final byte[] lastIID = storage.getLastKey(prefix);
                ruleKey = lastIID != null ?
                        new AtomicInteger(wrap(copyOfRange(lastIID, PrefixIID.LENGTH, VertexIID.Rule.LENGTH)).getShort() + delta) :
                        new AtomicInteger(initialValue);
            }
        }
    }

    public abstract static class Data {

        protected AtomicInteger ruleKey;
        protected final ConcurrentMap<PrefixIID, AtomicInteger> typeKeys;
        protected final ConcurrentMap<VertexIID.Schema, AtomicLong> thingKeys;
        protected final int initialValue;
        protected final int delta;

        Data(final int initialValue, final int delta) {
            typeKeys = new ConcurrentHashMap<>();
            ruleKey = new AtomicInteger(initialValue);
            thingKeys = new ConcurrentHashMap<>();
            this.initialValue = initialValue;
            this.delta = delta;
        }

        public byte[] forThing(final VertexIID.Schema schemaIID) {
            return longToSortedBytes(thingKeys.computeIfAbsent(
                    schemaIID, k -> new AtomicLong(initialValue)
            ).getAndAdd(delta));
        }

        public static class Buffered extends Data {

            public Buffered() {
                super(Encoding.Key.BUFFERED.initialValue(), Encoding.Key.BUFFERED.isIncrement() ? 1 : -1);
            }
        }

        public static class Persisted extends Data {

            public Persisted() {
                super(Encoding.Key.PERSISTED.initialValue(), Encoding.Key.PERSISTED.isIncrement() ? 1 : -1);
            }

            public void sync(final Storage storage) {
                final Encoding.Vertex.Thing[] thingsWithGeneratedIID = new Encoding.Vertex.Thing[]{
                        Encoding.Vertex.Thing.ENTITY, Encoding.Vertex.Thing.RELATION, Encoding.Vertex.Thing.ROLE
                };

                for (Encoding.Vertex.Thing thingEncoding : thingsWithGeneratedIID) {
                    final byte[] typeEncoding = Encoding.Vertex.Type.of(thingEncoding).prefix().bytes();
                    final ResourceIterator<byte[]> typeIterator = storage.iterate(typeEncoding, (iid, value) -> iid).filter(iid1 -> iid1.length == VertexIID.Schema.LENGTH);
                    while (typeIterator.hasNext()) {
                        final byte[] typeIID = typeIterator.next();
                        final byte[] prefix = join(thingEncoding.prefix().bytes(), typeIID);
                        final byte[] lastIID = storage.getLastKey(prefix);
                        final AtomicLong nextValue = lastIID != null ?
                                new AtomicLong(wrap(
                                        copyOfRange(lastIID, VertexIID.Thing.PREFIX_W_TYPE_LENGTH, VertexIID.Thing.DEFAULT_LENGTH)
                                ).getShort() + delta) :
                                new AtomicLong(initialValue);
                        thingKeys.put(VertexIID.Schema.of(typeIID), nextValue);
                    }
                }
            }
        }
    }
}
