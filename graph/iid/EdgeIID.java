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

package grakn.core.graph.iid;

import grakn.core.graph.util.Encoding;

import static grakn.core.common.collection.Bytes.join;
import static java.util.Arrays.copyOfRange;

public abstract class EdgeIID<
        EDGE_ENCODING extends Encoding.Edge,
        EDGE_INFIX extends InfixIID<EDGE_ENCODING>,
        VERTEX_IID_START extends VertexIID,
        VERTEX_IID_END extends VertexIID> extends IID {

    EDGE_INFIX infix;
    VERTEX_IID_START start;
    VERTEX_IID_END end;
    SuffixIID suffix;
    private int endIndex, infixIndex, suffixIndex;

    EdgeIID(final byte[] bytes) {
        super(bytes);
    }

    public abstract EDGE_INFIX infix();

    public abstract VERTEX_IID_START start();

    public abstract VERTEX_IID_END end();

    int infixIndex() {
        if (infixIndex == 0) infixIndex = start().bytes.length;
        return infixIndex;
    }

    int endIndex() {
        if (endIndex == 0) endIndex = infixIndex() + infix().bytes.length;
        return endIndex;
    }

    int suffixIndex() {
        if (suffixIndex == 0) suffixIndex = endIndex() + end().bytes.length;
        return suffixIndex;
    }

    public EDGE_ENCODING encoding() {
        return infix().encoding();
    }

    public boolean isOutwards() {
        return infix().isOutwards();
    }

    @Override
    public String toString() {
        if (readableString == null) {
            readableString = "[" + start().bytes.length + ": " + start().toString() + "]" +
                    "[" + infix().length() + ": " + infix().toString() + "]" +
                    "[" + end().bytes.length + ": " + end().toString() + "]";
        }
        return readableString;
    }

    public static class Schema extends EdgeIID<Encoding.Edge.Schema, InfixIID.Schema, VertexIID.Schema, VertexIID.Schema> {

        Schema(final byte[] bytes) {
            super(bytes);
        }

        public static Schema of(final byte[] bytes) {
            return new Schema(bytes);
        }

        public static Schema of(final VertexIID.Schema start, final Encoding.Infix infix, final VertexIID.Schema end) {
            return new Schema(join(start.bytes, infix.bytes(), end.bytes));
        }

        @Override
        public InfixIID.Schema infix() {
            if (infix == null) infix = InfixIID.Schema.extract(bytes, VertexIID.Schema.LENGTH);
            return infix;
        }

        @Override
        public VertexIID.Schema start() {
            if (start == null) start = VertexIID.Schema.of(copyOfRange(bytes, 0, VertexIID.Schema.LENGTH));
            return start;
        }

        @Override
        public VertexIID.Schema end() {
            if (end != null) return end;
            end = VertexIID.Schema.of(copyOfRange(bytes, bytes.length - VertexIID.Schema.LENGTH, bytes.length));
            return end;
        }
    }

    public static class Thing extends EdgeIID<Encoding.Edge.Thing, InfixIID.Thing, VertexIID.Thing, VertexIID.Thing> {

        Thing(final byte[] bytes) {
            super(bytes);
        }

        public static Thing of(final byte[] bytes) {
            return new Thing(bytes);
        }

        public static Thing of(final VertexIID.Thing start, final InfixIID.Thing infix, final VertexIID.Thing end) {
            return new Thing(join(start.bytes(), infix.bytes(), end.bytes()));
        }

        public static Thing of(final VertexIID.Thing start, final InfixIID.Thing infix, final VertexIID.Thing end, final SuffixIID suffix) {
            return new Thing(join(start.bytes(), infix.bytes(), end.bytes(), suffix.bytes()));
        }

        @Override
        public InfixIID.Thing infix() {
            if (infix == null) infix = InfixIID.Thing.extract(bytes, infixIndex());
            return infix;
        }

        public SuffixIID suffix() {
            if (suffix == null) suffix = SuffixIID.of(copyOfRange(bytes, suffixIndex(), bytes.length));
            return suffix;
        }

        @Override
        public VertexIID.Thing start() {
            if (start == null) start = VertexIID.Thing.extract(bytes, 0);

            return start;
        }

        @Override
        public VertexIID.Thing end() {
            if (end == null) end = VertexIID.Thing.extract(bytes, endIndex());
            return end;
        }

        @Override
        public String toString() {
            if (readableString == null) {
                readableString = super.toString();
                if (!suffix().isEmpty()) {
                    readableString += "[" + suffix().bytes.length + ": " + suffix().toString() + "]";
                }
            }
            return readableString;
        }
    }

    public static class InwardsISA extends EdgeIID<Encoding.Edge.Thing, InfixIID.Thing, VertexIID.Type, VertexIID.Thing> {

        private VertexIID.Type start;
        private VertexIID.Thing end;

        InwardsISA(final byte[] bytes) {
            super(bytes);
        }

        public static InwardsISA of(final byte[] bytes) {
            return new InwardsISA(bytes);
        }

        public static InwardsISA of(final VertexIID.Type start, final VertexIID.Thing end) {
            return new InwardsISA(join(start.bytes, Encoding.Edge.ISA.in().bytes(), end.bytes));
        }

        @Override
        public InfixIID.Thing infix() {
            return InfixIID.Thing.of(Encoding.Edge.ISA.in());
        }

        @Override
        public VertexIID.Type start() {
            if (start != null) return start;
            start = VertexIID.Type.of(copyOfRange(bytes, 0, VertexIID.Type.LENGTH));
            return start;
        }

        @Override
        public VertexIID.Thing end() {
            if (end != null) return end;
            end = VertexIID.Thing.of(copyOfRange(bytes, VertexIID.Type.LENGTH + 1, bytes.length));
            return end;
        }
    }
}
