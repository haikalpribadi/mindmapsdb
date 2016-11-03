/*
 * MindmapsDB - A Distributed Semantic Database
 * Copyright (C) 2016  Mindmaps Research Ltd
 *
 * MindmapsDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MindmapsDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MindmapsDB. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package io.mindmaps.test.graql.examples;

import io.mindmaps.graql.Graql;
import io.mindmaps.graql.InsertQuery;
import io.mindmaps.graql.MatchQuery;
import io.mindmaps.graql.QueryBuilder;
import io.mindmaps.test.AbstractRollbackGraphTest;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ExamplesTest extends AbstractRollbackGraphTest {

    private QueryBuilder qb;

    @Before
    public void setUp() {
        qb = Graql.withGraph(graph);
    }

    @Test
    public void testPhilosophers() {
        load(
                "insert person isa entity-type;",
                "insert name isa resource-type, datatype string;",
                "insert person has-resource name;",
                "insert isa person, has name 'Socrates';",
                "insert isa person, has name 'Plato';",
                "insert isa person, has name 'Aristotle';",
                "insert isa person, has name 'Alexander';"
        );

        assertEquals(4, qb.<MatchQuery>parse("match $p isa person;").stream().count());

        load(
                "insert school isa entity-type, has-resource name;",
                "insert isa school, has name 'Peripateticism';",
                "insert isa school, has name 'Platonism';",
                "insert isa school, has name 'Idealism';",
                "insert isa school, has name 'Cynicism';"
        );

        assertEquals(1, qb.<MatchQuery>parse("match $x has name 'Cynicism';").stream().count());

        load(
                "insert practice isa relation-type;",
                "insert philosopher isa role-type;",
                "insert philosophy isa role-type;",
                "insert practice has-role philosopher, has-role philosophy;",
                "insert person plays-role philosopher;",
                "insert school plays-role philosophy;",
                "match $socrates has name 'Socrates'; $platonism has name 'Platonism'; insert (philosopher: $socrates, philosophy: $platonism) isa practice;",
                "match $plato has name 'Plato'; $idealism has name 'Idealism'; insert (philosopher: $plato, philosophy: $idealism) isa practice;",
                "match $plato has name 'Plato'; $platonism has name 'Platonism'; insert (philosopher: $plato, philosophy: $platonism) isa practice;",
                "match $aristotle has name 'Aristotle'; $peripateticism has name 'Peripateticism'; insert (philosopher: $aristotle, philosophy: $peripateticism) isa practice;"
        );

        assertEquals(
                2,
                qb.<MatchQuery>parse("match (philosopher: $x, $platonism) isa practice; $platonism has name 'Platonism';").stream().count()
        );

        load(
                "insert education isa relation-type;",
                "insert teacher isa role-type;",
                "insert student isa role-type;",
                "insert education has-role teacher, has-role student;",
                "insert person plays-role teacher, plays-role student;",
                "match $socrates has name 'Socrates'; $plato has name 'Plato'; insert (teacher: $socrates, student: $plato) isa education;",
                "match $plato has name 'Plato'; $aristotle has name 'Aristotle'; insert (teacher: $plato, student: $aristotle) isa education;",
                "match $aristotle has name 'Aristotle'; $alexander has name 'Alexander'; insert (teacher: $aristotle, student: $alexander) isa education;"
        );

        load(
                "insert title isa resource-type, datatype string;",
                "insert epithet isa resource-type, datatype string;",
                "insert person has-resource title;",
                "insert person has-resource epithet;"
        );

        load(
                "match $alexander has name 'Alexander'; insert $alexander has epithet 'The Great';",
                "match $alexander has name 'Alexander'; insert $alexander has title 'Hegemon';",
                "match $alexander has name 'Alexander'; insert $alexander has title 'King of Macedon';",
                "match $alexander has name 'Alexander'; insert $alexander has title 'Shah of Persia';",
                "match $alexander has name 'Alexander'; insert $alexander has title 'Pharaoh of Egypt';",
                "match $alexander has name 'Alexander'; insert $alexander has title 'Lord of Asia';"
        );

        MatchQuery pharaoh = qb.parse("match has name $x, has title contains 'Pharaoh';");
        assertEquals("Alexander", pharaoh.iterator().next().get("x").asResource().getValue());

        load(
                "insert knowledge isa relation-type;",
                "insert thinker isa role-type;",
                "insert thought isa role-type;",
                "insert knowledge has-role thinker, has-role thought;",
                "insert fact isa entity-type, plays-role thought;",
                "insert description isa resource-type, datatype string;",
                "insert fact has-resource name, has-resource description;",
                "insert person plays-role thinker;",
                "insert isa fact, has name 'sun-fact', has description 'The Sun is bigger than the Earth';",
                "match $aristotle has name 'Aristotle'; $sun-fact has name 'sun-fact'; insert (thinker: $aristotle, thought: $sun-fact) isa knowledge;",
                "insert isa fact, has name 'cave-fact', has description 'Caves are mostly pretty dark';",
                "match $plato has name 'Plato'; $cave-fact has name 'cave-fact'; insert (thinker: $plato, thought: $cave-fact) isa knowledge;",
                "insert isa fact, has name 'nothing';",
                "match $socrates has name 'Socrates'; $nothing has name 'nothing'; insert (thinker: $socrates, thought: $nothing) isa knowledge;"
        );

        load(
                "insert knowledge plays-role thought;",
                "match $socrates has name 'Socrates'; $nothing has name 'nothing'; $socratesKnowsNothing ($socrates, $nothing); " +
                "insert (thinker: $socrates, thought: $socratesKnowsNothing) isa knowledge;"
        );

        assertEquals(
                2,
                qb.<MatchQuery>parse("match $socrates has name 'Socrates'; ($socrates, $x) isa knowledge;").stream().count()
        );
    }

    private void load(String... queries) {
        Stream.of(queries).map((queryString) -> qb.<InsertQuery>parse(queryString)).forEach(InsertQuery::execute);
    }
}
