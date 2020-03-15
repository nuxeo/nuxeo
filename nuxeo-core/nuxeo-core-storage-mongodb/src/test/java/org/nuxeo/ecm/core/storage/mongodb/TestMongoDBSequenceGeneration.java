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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.COUNTER_FIELD;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.COUNTER_NAME_UUID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;

import javax.inject.Inject;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase.IdType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerWrapper;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

@RunWith(FeaturesRunner.class)
@Features({ MongoDBFeature.class, TransactionalFeature.class })
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.storage")
public class TestMongoDBSequenceGeneration {

    @Inject
    protected MongoDBConnectionService connectionService;

    protected MongoDBRepository repo1;

    protected MongoDBRepository repo2;

    public void setUp(IdType idType) {
        ConnectionManagerWrapper cm = mock(ConnectionManagerWrapper.class);
        doNothing().when(cm).dispose();

        MongoDBRepositoryDescriptor desc = new MongoDBRepositoryDescriptor();
        desc.name = Framework.getProperty(MongoDBFeature.MONGODB_DBNAME_PROPERTY);
        desc.idType = idType.name();
        desc.sequenceBlockSize = 5;

        // clear counters
        MongoDatabase database = connectionService.getDatabase(MongoDBRepository.REPOSITORY_CONNECTION_PREFIX + desc.name);
        MongoCollection<Document> countersColl = database.getCollection(desc.name + ".counters");
        countersColl.deleteMany(new Document());

        repo1 = new MongoDBRepository(cm, desc);
        repo2 = new MongoDBRepository(cm, desc);
    }

    protected void assertNext(String expected, MongoDBRepository repo) {
        assertEquals(expected, repo.generateNewId());
    }

    protected void setCounter(MongoDBRepository repo, long value) {
        repo.countersColl.updateOne(Filters.eq(MONGODB_ID, COUNTER_NAME_UUID),
                Updates.set(COUNTER_FIELD, Long.valueOf(value)));
    }

    protected long getCounter(MongoDBRepository repo) {
        Bson filter = Filters.eq(MONGODB_ID, COUNTER_NAME_UUID);
        return repo.countersColl.find(filter).first().getLong(COUNTER_FIELD);
    }

    @Test
    public void testSequence() {
        setUp(IdType.sequence);

        // fake a previous non-0 counter
        setCounter(repo1, 100);

        assertNext("101", repo1);
        assertNext("102", repo1);
        // switch to repo 2
        assertNext("106", repo2);
        assertNext("107", repo2);
        // back to repo 1
        assertNext("103", repo1);
        assertNext("104", repo1);
        assertNext("105", repo1);
        // repo1 starting next block
        assertNext("111", repo1);
        // switch to repo 2
        assertNext("108", repo2);
        assertNext("109", repo2);
        assertNext("110", repo2);
        // repo2 starting next block
        assertNext("116", repo2);
    }

    @Test
    public void testSequenceRandomized() {
        setUp(IdType.sequenceHexRandomized);

        // use a known initial seed for reproducibility
        long v0 = 0x1234567890abcdefL;
        // sequence from the seed
        long v1 = xorshift(v0);
        long v2 = xorshift(v1);
        long v3 = xorshift(v2);
        long v4 = xorshift(v3);
        long v5 = xorshift(v4);
        long v6 = xorshift(v5);
        long v7 = xorshift(v6);
        long v8 = xorshift(v7);
        long v9 = xorshift(v8);
        long v10 = xorshift(v9);
        long v11 = xorshift(v10);
        long v12 = xorshift(v11);
        long v13 = xorshift(v12);
        long v14 = xorshift(v13);
        long v15 = xorshift(v14);
        long v16 = xorshift(v15);
        assertEquals(0xfc00d76d31ac01b4L, v1);
        assertEquals(0xb054aa496997b4b7L, v2);
        assertEquals(0x5dbd6c0bc403561eL, v3);
        assertEquals(0x28991f9897f91732L, v4);
        assertEquals(0xb9862d97a94d699cL, v5);
        assertEquals(0x1ed241e350e8144fL, v6);
        assertEquals(0xb910aa23c18b37a7L, v7);
        assertEquals(0x5a617fd4d2212808L, v8);
        assertEquals(0xd1c77fc7067e6858L, v9);
        assertEquals(0xfe2e55f1dff38288L, v10);
        assertEquals(0xa95e2f283fe7c78dL, v11);
        assertEquals(0xd16883b5f81c4b42L, v12);
        assertEquals(0xc1023be2db3ee354L, v13);
        assertEquals(0xbffa86f19a7ecb92L, v14);
        assertEquals(0x175083c01a809285L, v15);
        assertEquals(0xa40480120d0192e0L, v16);

        setCounter(repo1, v0);

        assertNext("fc00d76d31ac01b4", repo1);
        assertEquals(v5, getCounter(repo1));
        assertNext("b054aa496997b4b7", repo1);
        // switch to repo 2
        assertNext("1ed241e350e8144f", repo2);
        assertNext("b910aa23c18b37a7", repo2);
        // back to repo 1
        assertNext("5dbd6c0bc403561e", repo1);
        assertNext("28991f9897f91732", repo1);
        assertNext("b9862d97a94d699c", repo1);
        assertEquals(v10, getCounter(repo1));
        // repo1 starting next block
        assertNext("a95e2f283fe7c78d", repo1);
        // switch to repo 2
        assertNext("5a617fd4d2212808", repo2);
        assertNext("d1c77fc7067e6858", repo2);
        assertNext("fe2e55f1dff38288", repo2);
        // repo2 starting next block
        assertEquals(v15, getCounter(repo1));
        assertNext("a40480120d0192e0", repo2);
    }

    protected static long xorshift(long n) {
        n ^= (n << 13);
        n ^= (n >>> 7);
        n ^= (n << 17);
        return n;
    }

}
