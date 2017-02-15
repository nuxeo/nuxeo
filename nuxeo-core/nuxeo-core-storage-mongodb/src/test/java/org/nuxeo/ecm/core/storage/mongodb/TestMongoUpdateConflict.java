/*
 * Copyright (c) 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_SET;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBConverter.UpdateBuilder;

public class TestMongoUpdateConflict {

    protected UpdateBuilder newUpdateBuilder() {
        UpdateBuilder ub = new MongoDBConverter(KEY_ID).new UpdateBuilder();
        ub.newUpdate();
        return ub;
    }

    @Test
    public void testEmpty() throws Exception {
        UpdateBuilder ulb = newUpdateBuilder();
        assertEquals(1, ulb.updates.size());
    }

    @Test
    public void testComplexPropNoConflict() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar", "val");
        ub.update(MONGODB_SET, "foo.0.zoo", "val");
        assertEquals(1, ub.updates.size());
    }

    @Test
    public void testComplexPropNoConflict2() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar.zoo.x", "val");
        ub.update(MONGODB_SET, "foo.0.bar.zoo.y", "val");
        assertEquals(1, ub.updates.size());
    }

    @Test
    public void testComplexPropNoConflict3() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar.zoo.x", "val");
        ub.update(MONGODB_SET, "foo.0.bar.z", "val");
        assertEquals(1, ub.updates.size());
    }

    @Test
    public void testComplexPropNoConflict4() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar.zoo", "val");
        ub.update(MONGODB_SET, "foo.00.bar", "val");
        ub.update(MONGODB_SET, "foobar", "val");
        ub.update(MONGODB_SET, "foobar1.0.bar.zoo", "val");
        ub.update(MONGODB_SET, "foo.1.0.bar.zoo", "val");
        ub.update(MONGODB_SET, "foo.bar", "val");
        ub.update(MONGODB_SET, "bar.foo", "val");
        ub.update(MONGODB_SET, "zz", "val");
        assertEquals(1, ub.updates.size());
    }

    @Test
    public void testConflictEqual() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar", "val");
        ub.update(MONGODB_SET, "foo.0.bar", "val2");
        assertEquals(2, ub.updates.size());
    }

    @Test
    public void testConflictEqual2() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo", "val");
        ub.update(MONGODB_SET, "foo", "val2");
        assertEquals(2, ub.updates.size());
    }

    @Test
    public void testConflict1() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar", "val");
        ub.update(MONGODB_SET, "foo.0.bar.zoo", "val");
        assertEquals(2, ub.updates.size());
    }

    @Test
    public void testConflict2() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar.zoo", "val");
        ub.update(MONGODB_SET, "foo.0.bar", "val");
        assertEquals(2, ub.updates.size());
    }

    @Test
    public void testConflict3() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        ub.update(MONGODB_SET, "foo.0.bar.zoo", "val");
        ub.update(MONGODB_SET, "foo", "val");
        assertEquals(2, ub.updates.size());
    }

    @Test
    public void testBigComplex() throws Exception {
        UpdateBuilder ub = newUpdateBuilder();
        for (int i = 0; i < 10000; i++) {
            ub.update(MONGODB_SET, "foo." + i + ".bar.zoo", "val");
        }
        assertEquals(1, ub.updates.size());
    }


}
