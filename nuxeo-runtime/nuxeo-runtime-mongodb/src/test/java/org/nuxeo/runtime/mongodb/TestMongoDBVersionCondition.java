/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.runtime.mongodb;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.runtime.mongodb.MongoDBComponent.MongoDBCountHelper.countDocuments;

import java.util.Collections;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ MongoDBFeature.class })
public class TestMongoDBVersionCondition {

    private static final String DEFAULT_VALUE = "myValue";

    private static final String DEFAULT_KEY = "myKey";

    @Test
    public void testCountCall() {
        MongoDatabase database = Framework.getService(MongoDBConnectionService.class).getDatabase("default");
        MongoCollection<Document> coll = database.getCollection("myCollection");
        Bson filter = new Document().append(DEFAULT_KEY, DEFAULT_VALUE);
        coll.insertOne(new Document(Collections.singletonMap(DEFAULT_KEY, DEFAULT_VALUE)));
        coll.insertOne(new Document(Collections.singletonMap(DEFAULT_KEY, DEFAULT_VALUE)));
        coll.insertOne(new Document(Collections.singletonMap("myHey", "myFalue")));
        CountOptions options = new CountOptions().limit(1);
        assertEquals(3, countDocuments(database, coll));
        assertEquals(2, countDocuments(database, coll, filter));
        assertEquals(1, countDocuments(database, coll, filter, options));
    }
}
