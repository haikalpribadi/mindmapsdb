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

package ai.grakn.graql.internal.printer;

import ai.grakn.concept.Concept;
import ai.grakn.concept.SchemaConcept;
import ai.grakn.graql.ComputeQuery;
import ai.grakn.graql.Pattern;
import ai.grakn.graql.Var;
import ai.grakn.util.CommonUtil;
import mjson.Json;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toList;

class JsonPrinter extends Printer<Json> {
    @Override
    public final String complete(Json builder) {
        return builder.toString();
    }

    @Override
    public Json concept(Concept concept) {
        Json json = Json.object("id", concept.getId().getValue());

        if (concept.isSchemaConcept()) {
            json.set("name", concept.asSchemaConcept().getLabel().getValue());
            SchemaConcept superConcept = concept.asSchemaConcept().sup();
            if (superConcept != null) json.set("sub", superConcept.getLabel().getValue());
        } else if (concept.isThing()) {
            json.set("isa", concept.asThing().type().getLabel().getValue());
        } else {
            throw CommonUtil.unreachableStatement("Unrecognised concept " + concept);
        }

        if (concept.isAttribute()) {
            json.set("value", concept.asAttribute().getValue());
        }

        if (concept.isRule()) {
            Pattern when = concept.asRule().getWhen();
            if (when != null) {
                json.set("when", when.toString());
            }
            Pattern then = concept.asRule().getThen();
            if (then != null) {
                json.set("then", then.toString());
            }
        }

        return json;
    }

    @Override
    public final Json bool(boolean bool) {
        return Json.make(bool);
    }

    @Override
    public final Json collection(Collection<?> collection) {
        return Json.make(collection.stream().map(item -> build(item)).collect(toList()));
    }

    @Override
    public final Json map(Map<?, ?> map) {
        Json json = Json.object();

        map.forEach((Object key, Object value) -> {
            if (key instanceof Var) key = ((Var) key).getValue();
            String keyString = key == null ? "" : key.toString();
            json.set(keyString, build(value));
        });

        return json;
    }

    //TODO: Implement JsonPrinter for ComputeAnswer properly!
    @Override
    public Json computeAnswer(ComputeQuery.Answer computeAnswer) {
        return object(computeAnswer);
    }

    @Override
    public final Json object(Object object) {
        return Json.make(object);
    }
}
