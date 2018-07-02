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

package ai.grakn.client.rpc;

import ai.grakn.GraknTxType;
import ai.grakn.concept.AttributeType;
import ai.grakn.concept.ConceptId;
import ai.grakn.concept.Label;
import ai.grakn.graql.Pattern;
import ai.grakn.graql.Query;
import ai.grakn.rpc.proto.IteratorProto.IteratorId;
import ai.grakn.rpc.proto.IteratorProto.Next;
import ai.grakn.rpc.proto.IteratorProto.Stop;
import ai.grakn.rpc.proto.KeyspaceProto;
import ai.grakn.rpc.proto.SessionProto;

/**
 * A utility class to build RPC Requests from a provided set of Grakn concepts.
 */
public class RequestBuilder {

    /**
     * An RPC Request Builder class for Transaction Service
     */
    public static class Transaction {

        public static SessionProto.Transaction.Req open(ai.grakn.Keyspace keyspace, GraknTxType txType) {
            SessionProto.Open.Req openRequest = SessionProto.Open.Req.newBuilder()
                    .setKeyspace(keyspace.getValue())
                    .setTxType(txType.getId())
                    .build();

            return SessionProto.Transaction.Req.newBuilder().setOpen(openRequest).build();
        }

        public static SessionProto.Transaction.Req commit() {
            return SessionProto.Transaction.Req.newBuilder()
                    .setCommit(SessionProto.Commit.Req.getDefaultInstance())
                    .build();
        }

        public static SessionProto.Transaction.Req query(Query<?> query) {
            return query(query.toString(), query.inferring());
        }

        public static SessionProto.Transaction.Req query(String queryString, boolean infer) {
            SessionProto.Query.Req request = SessionProto.Query.Req.newBuilder()
                    .setQuery(queryString)
                    .setInfer(infer)
                    .build();
            return SessionProto.Transaction.Req.newBuilder().setQuery(request).build();
        }

        public static SessionProto.Transaction.Req getSchemaConcept(Label label) {
            return SessionProto.Transaction.Req.newBuilder()
                    .setGetSchemaConcept(SessionProto.GetSchemaConcept.Req.newBuilder().setLabel(label.getValue()))
                    .build();
        }

        public static SessionProto.Transaction.Req getConcept(ConceptId id) {
            return SessionProto.Transaction.Req.newBuilder()
                    .setGetConcept(SessionProto.GetConcept.Req.newBuilder().setId(id.getValue()))
                    .build();
        }


        public static SessionProto.Transaction.Req getAttributes(Object value) {
            return SessionProto.Transaction.Req.newBuilder()
                    .setGetAttributes(SessionProto.GetAttributes.Req.newBuilder()
                            .setValue(ConceptBuilder.attributeValue(value))
                    ).build();
        }

        public static SessionProto.Transaction.Req putEntityType(Label label) {
            return SessionProto.Transaction.Req.newBuilder()
                    .setPutEntityType(SessionProto.PutEntityType.Req.newBuilder().setLabel(label.getValue()))
                    .build();
        }

        public static SessionProto.Transaction.Req putAttributeType(Label label, AttributeType.DataType<?> dataType) {
            SessionProto.PutAttributeType.Req request = SessionProto.PutAttributeType.Req.newBuilder()
                    .setLabel(label.getValue())
                    .setDataType(ConceptBuilder.dataType(dataType))
                    .build();

            return SessionProto.Transaction.Req.newBuilder().setPutAttributeType(request).build();
        }

        public static SessionProto.Transaction.Req putRelationshipType(Label label) {
            SessionProto.PutRelationshipType.Req request = SessionProto.PutRelationshipType.Req.newBuilder()
                    .setLabel(label.getValue())
                    .build();
            return SessionProto.Transaction.Req.newBuilder().setPutRelationshipType(request).build();
        }

        public static SessionProto.Transaction.Req putRole(Label label) {
            SessionProto.PutRole.Req request = SessionProto.PutRole.Req.newBuilder()
                    .setLabel(label.getValue())
                    .build();
            return SessionProto.Transaction.Req.newBuilder().setPutRole(request).build();
        }

        public static SessionProto.Transaction.Req putRule(Label label, Pattern when, Pattern then) {
            SessionProto.PutRule.Req request = SessionProto.PutRule.Req.newBuilder()
                    .setLabel(label.getValue())
                    .setWhen(when.toString())
                    .setThen(then.toString())
                    .build();
            return SessionProto.Transaction.Req.newBuilder().setPutRule(request).build();
        }

        public static SessionProto.Transaction.Req next(IteratorId iteratorId) {
            return SessionProto.Transaction.Req.newBuilder().setNext(Next.newBuilder().setIteratorId(iteratorId)).build();
        }

        public static SessionProto.Transaction.Req stop(IteratorId iteratorId) {
            return SessionProto.Transaction.Req.newBuilder().setStop(Stop.newBuilder().setIteratorId(iteratorId)).build();
        }
    }

    /**
     * An RPC Request Builder class for Keyspace Service
     */
    public static class Keyspace {

        public static KeyspaceProto.Delete.Req delete(String name) {
            return KeyspaceProto.Delete.Req.newBuilder().setName(name).build();
        }
    }
}
