/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

public class TestMongoDBRepositoryDescriptor {

    @Test
    public void testMerge() {
        MongoDBRepositoryDescriptor desc1 = new MongoDBRepositoryDescriptor();
        desc1.label = "1";
        desc1.sequenceBlockSize = Integer.valueOf(1);
        MongoDBRepositoryDescriptor desc2 = new MongoDBRepositoryDescriptor();
        desc2.label = "2";
        desc2.sequenceBlockSize = Integer.valueOf(2);

        // call merge using the generic signature (to check it's correctly overridden)
        // this is how DBSRepositoryDescriptorRegistry calls it
        desc1.merge((DBSRepositoryDescriptor) desc2);
        assertEquals("2", desc1.label);
        assertEquals(Integer.valueOf(2), desc1.sequenceBlockSize);
    }

}
