/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Delbosc Benoit
 */
package org.nuxeo.ecm.core.redis;

import org.junit.Test;
import org.nuxeo.ecm.core.redis.contribs.RedisInvalidations;
import org.nuxeo.ecm.core.storage.sql.VCSInvalidations;
import org.nuxeo.ecm.core.storage.sql.RowId;

import static org.junit.Assert.*;

/**
 * @since 7.4
 */
public class TestRedisInvalidations {
    @Test
    public void testRedisInvalidations() throws Exception {
        VCSInvalidations invals = new VCSInvalidations();
        invals.addModified(new RowId("dublincore", "docid1"));

        RedisInvalidations srcInvals = new RedisInvalidations("node1", invals);
        assertEquals("RedisInvalidationsInvalidations(fromNode=node1, VCSInvalidations(modified=[RowId(dublincore, docid1)]))",
                srcInvals.toString());

        RedisInvalidations destInvals = new RedisInvalidations("node1", srcInvals.serialize());
        assertEquals("RedisInvalidationsInvalidations(local, discarded)", destInvals.toString());
        assertNull(destInvals.getInvalidations());

        destInvals = new RedisInvalidations("node2", srcInvals.serialize());
        assertNotNull(destInvals.getInvalidations());
        assertEquals(invals.toString(), destInvals.getInvalidations().toString());
    }

}
