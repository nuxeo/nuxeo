package org.nuxeo.ecm.core.redis;

import org.junit.Test;
import org.nuxeo.ecm.core.redis.contribs.RedisInvalidations;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.RowId;

import static org.junit.Assert.*;
/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */

/**
 * @since 7.4
 */
public class TestRedisInvalidations {
    @Test
    public void testRedisInvalidations() throws Exception {
        Invalidations invals = new Invalidations();
        invals.addModified(new RowId("dublincore", "docid1"));

        RedisInvalidations srcInvals = new RedisInvalidations("node1", invals);
        assertEquals("RedisInvalidationsInvalidations(fromNode=node1, Invalidations(modified=[RowId(dublincore, docid1)]))",
                srcInvals.toString());

        RedisInvalidations destInvals = new RedisInvalidations("node1", srcInvals.serialize());
        assertEquals("RedisInvalidationsInvalidations(local, discarded)", destInvals.toString());
        assertNull(destInvals.getInvalidations());

        destInvals = new RedisInvalidations("node2", srcInvals.serialize());
        assertNotNull(destInvals.getInvalidations());
        assertEquals(invals.toString(), destInvals.getInvalidations().toString());
    }

}