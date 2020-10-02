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

package grakn.core.common.parameters;

import grakn.core.common.exception.ErrorMessage;
import grakn.core.common.exception.GraknException;

public abstract class Options<PARENT extends Options<?, ?>, SELF extends Options<?, ?>> {

    public static final boolean DEFAULT_INFER = true;
    public static final boolean DEFAULT_EXPLAIN = false;
    public static final int DEFAULT_BATCH_SIZE = 50;

    private PARENT parent;
    private Boolean infer = null;
    private Boolean explain = null;
    private Integer batchSize = null;

    abstract SELF getThis();

    public SELF parent(final PARENT parent) {
        this.parent = parent;
        return getThis();
    }

    public Boolean infer() {
        if (infer != null) {
            return infer;
        } else if (parent != null) {
            return parent.infer();
        } else {
            return DEFAULT_INFER;
        }
    }

    public SELF infer(final boolean infer) {
        this.infer = infer;
        return getThis();
    }

    public Boolean explain() {
        if (explain != null) {
            return explain;
        } else if (parent != null) {
            return parent.explain();
        } else {
            return DEFAULT_EXPLAIN;
        }
    }

    public SELF explain(final boolean explain) {
        this.explain = explain;
        return getThis();
    }

    public Integer batchSize() {
        if (batchSize != null) {
            return batchSize;
        } else if (parent != null) {
            return parent.batchSize();
        } else {
            return DEFAULT_BATCH_SIZE;
        }
    }

    public SELF batchSize(final int batchSize) {
        this.batchSize = batchSize;
        return getThis();
    }

    public static class Database extends Options<Options<?, ?>, Database> {

        @Override
        Database getThis() {
            return this;
        }

        public Database parent(final Options<?, ?> parent) {
            throw new GraknException(ErrorMessage.Internal.ILLEGAL_ARGUMENT);
        }
    }

    public static class Session extends Options<Database, Session> {

        @Override
        Session getThis() {
            return this;
        }
    }

    public static class Transaction extends Options<Session, Transaction> {

        @Override
        Transaction getThis() {
            return this;
        }
    }

    public static class Query extends Options<Transaction, Query> {

        @Override
        Query getThis() {
            return this;
        }
    }
}
