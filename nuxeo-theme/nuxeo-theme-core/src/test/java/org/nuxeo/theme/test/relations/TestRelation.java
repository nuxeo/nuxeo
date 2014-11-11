/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.relations;

import junit.framework.TestCase;

import org.nuxeo.theme.relations.DefaultPredicate;
import org.nuxeo.theme.relations.DefaultRelate;
import org.nuxeo.theme.relations.DyadicRelation;
import org.nuxeo.theme.relations.MonadicRelation;
import org.nuxeo.theme.relations.Predicate;
import org.nuxeo.theme.relations.Relate;
import org.nuxeo.theme.relations.Relation;
import org.nuxeo.theme.relations.RelationTypeFamily;
import org.nuxeo.theme.relations.TriadicRelation;

public class TestRelation extends TestCase {

    public void testMonadicRelation() {
        Predicate predicate = new DefaultPredicate("_ is white");
        Relate r1 = new DefaultRelate("snow");
        Relation relation = new MonadicRelation(predicate, r1);
        assertEquals(RelationTypeFamily.MONADIC,
                relation.getRelationTypeFamily());

        assertTrue(relation.hasPredicate(predicate));
        assertEquals("snow is white", relation.hash());
        assertTrue(relation.getRelates().contains(r1));
        assertEquals(r1, relation.getRelate(1));
    }

    public void testDyadicRelation() {
        Predicate predicate = new DefaultPredicate("_ loves _");
        Relate r1 = new DefaultRelate("Romeo");
        Relate r2 = new DefaultRelate("Juliet");
        Relation relation = new DyadicRelation(predicate, r1, r2);

        assertEquals(RelationTypeFamily.DYADIC,
                relation.getRelationTypeFamily());
        assertTrue(relation.hasPredicate(predicate));
        assertEquals("Romeo loves Juliet", relation.hash());
        assertTrue(relation.getRelates().contains(r1));
        assertTrue(relation.getRelates().contains(r2));
        assertEquals(r1, relation.getRelate(1));
        assertEquals(r2, relation.getRelate(2));
    }

    public void testTriadicRelation() {
        Predicate predicate = new DefaultPredicate("_ connects _ to _");
        Relate r1 = new DefaultRelate("A");
        Relate r2 = new DefaultRelate("B");
        Relate r3 = new DefaultRelate("C");
        Relation relation = new TriadicRelation(predicate, r1, r2, r3);

        assertEquals(RelationTypeFamily.TRIADIC,
                relation.getRelationTypeFamily());
        assertTrue(relation.hasPredicate(predicate));
        assertEquals("A connects B to C", relation.hash());
        assertTrue(relation.getRelates().contains(r1));
        assertTrue(relation.getRelates().contains(r2));
        assertTrue(relation.getRelates().contains(r3));
        assertEquals(r1, relation.getRelate(1));
        assertEquals(r2, relation.getRelate(2));
        assertEquals(r3, relation.getRelate(3));
    }

}
