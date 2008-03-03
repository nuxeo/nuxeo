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
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.relations.TriadicRelation;

public class TestStorage extends TestCase {

    private RelationStorage storage;

    @Override
    public void setUp() {
        storage = new RelationStorage();
    }

    public void testMonadicRelations() {
        Predicate predicate = new DefaultPredicate("_ is white");
        Relate r1 = new DefaultRelate("snow");
        Relation relation = new MonadicRelation(predicate, r1);

        assertFalse(storage.search(predicate, r1).contains(relation));
        assertFalse(storage.list().contains(relation));

        storage.add(relation);
        assertTrue(storage.list().contains(relation));
        assertTrue(storage.search(predicate, r1).contains(relation));

        storage.remove(relation);
        assertFalse(storage.search(predicate, r1).contains(relation));
        assertFalse(storage.list().contains(relation));
    }

    public void testDyadicRelations() {
        Predicate predicate = new DefaultPredicate("_ loves _");
        Relate r1 = new DefaultRelate("Romeo");
        Relate r2 = new DefaultRelate("Juliet");
        Relation relation = new DyadicRelation(predicate, r1, r2);

        assertFalse(storage.search(predicate, r1, r2).contains(relation));
        assertFalse(storage.list().contains(relation));

        storage.add(relation);
        assertTrue(storage.list().contains(relation));
        assertTrue(storage.list().contains(relation));
        assertTrue(storage.search(predicate, r1, r2).contains(relation));
        assertTrue(storage.search(predicate, null, r2).contains(relation));
        assertTrue(storage.search(predicate, r1, null).contains(relation));

        storage.remove(relation);
        assertFalse(storage.search(predicate, r1, r2).contains(relation));
        assertFalse(storage.list().contains(relation));
    }

    public void testTriadicRelations() {
        Predicate predicate = new DefaultPredicate("_ connects _ to _");
        Relate r1 = new DefaultRelate("A");
        Relate r2 = new DefaultRelate("B");
        Relate r3 = new DefaultRelate("C");
        Relation relation = new TriadicRelation(predicate, r1, r2, r3);

        assertFalse(storage.search(predicate, r1, r2, r3).contains(relation));
        assertFalse(storage.list().contains(relation));

        storage.add(relation);
        assertTrue(storage.search(predicate, r1, r2, r3).contains(relation));
        assertTrue(storage.search(predicate, null, r2, r3).contains(relation));
        assertTrue(storage.search(predicate, r1, null, r3).contains(relation));
        assertTrue(storage.search(predicate, r1, r2, null).contains(relation));
        assertTrue(storage.search(predicate, null, null, r3).contains(relation));
        assertTrue(storage.search(predicate, null, r2, null).contains(relation));
        assertTrue(storage.search(predicate, r1, null, null).contains(relation));
        assertTrue(storage.search(predicate, null, r2, null).contains(relation));

        storage.remove(relation);
        assertFalse(storage.search(predicate, r1, r2, r3).contains(relation));
        assertFalse(storage.list().contains(relation));
    }

}
