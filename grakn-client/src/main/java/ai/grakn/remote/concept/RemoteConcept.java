/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016-2018 Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/agpl.txt>.
 */

package ai.grakn.remote.concept;

import ai.grakn.Keyspace;
import ai.grakn.concept.AttributeType;
import ai.grakn.concept.Concept;
import ai.grakn.concept.ConceptId;
import ai.grakn.exception.GraknTxOperationException;
import ai.grakn.remote.RemoteGraknTx;
import ai.grakn.remote.rpc.Iterator;
import ai.grakn.rpc.generated.GrpcConcept;
import ai.grakn.rpc.generated.GrpcGrakn;
import ai.grakn.rpc.generated.GrpcIterator;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Felix Chapman
 */
abstract class RemoteConcept<SomeConcept extends Concept> implements Concept {

    abstract RemoteGraknTx tx();

    @Override
    public abstract ConceptId getId();

    @Override
    public final Keyspace keyspace() {
        return tx().keyspace();
    }

    @Override
    public final void delete() throws GraknTxOperationException {
        GrpcConcept.ConceptMethod.Builder method = GrpcConcept.ConceptMethod.newBuilder();
        method.setDelete(GrpcConcept.Unit.getDefaultInstance());
        runMethod(method.build());
    }

    @Override
    public final boolean isDeleted() {
        return tx().getConcept(getId()) == null;
    }

    protected final Stream<? extends Concept> runMethodToConceptStream(GrpcConcept.ConceptMethod method) {
        GrpcIterator.IteratorId iteratorId = runMethod(method).getConceptResponse().getIteratorId();
        Iterable<? extends Concept> iterable = () -> new Iterator<>(
                tx(), iteratorId, res -> tx().conceptReader().concept(res.getConcept())
        );

        return StreamSupport.stream(iterable.spliterator(), false);
    }
    protected final GrpcGrakn.TxResponse runMethod(GrpcConcept.ConceptMethod method) {
        return runMethod(getId(), method);
    }

    protected final GrpcGrakn.TxResponse runMethod(ConceptId id, GrpcConcept.ConceptMethod method) {
        return tx().runConceptMethod(id, method);
    }

    abstract SomeConcept asCurrentBaseType(Concept other);

    // TODO: There must be a better way than this. Figure out a way autoamtically mapping one side to another.
    public static AttributeType.DataType<?> dataType(GrpcConcept.DataType dataType) {
        switch (dataType) {
            case String:
                return AttributeType.DataType.STRING;
            case Boolean:
                return AttributeType.DataType.BOOLEAN;
            case Integer:
                return AttributeType.DataType.INTEGER;
            case Long:
                return AttributeType.DataType.LONG;
            case Float:
                return AttributeType.DataType.FLOAT;
            case Double:
                return AttributeType.DataType.DOUBLE;
            case Date:
                return AttributeType.DataType.DATE;
            default:
            case UNRECOGNIZED:
                throw new IllegalArgumentException("Unrecognised " + dataType);
        }
    }
}
