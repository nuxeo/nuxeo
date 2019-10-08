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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.nuxeo.runtime.mongodb.MongoDBComponent.MongoDBCountHelper.countDocuments;
import static org.nuxeo.runtime.mongodb.MongoDBComponent.MongoDBCountHelper.versions;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Test;
import org.nuxeo.runtime.test.runner.Features;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;

/**
 * @since 11.1
 */
@Features({ MongoDBFeature.class })
public class TestMongoDBVersionCondition {
    /**
     *
     */
    private static final String NEW_STUFF_NAMESPACE = "new.stuff";

    /**
     *
     */
    private static final String OLD_STUFF_NAMESPACE = "old.stuff";

    // a MongoDBComponent to add database versions
    class TestService extends MongoDBComponent {
        protected TestService() {
            versions.put("old", "3.4");
            versions.put("new", "3.8");
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCountCall() {
        // init versions
        new TestService();

        // old count
        MongoCollection<Document> coll = mockCollection(OLD_STUFF_NAMESPACE);
        verify(coll, times(0)).count(); // NOSONAR
        verify(coll, times(0)).countDocuments();
        countDocuments(coll);
        verify(coll, times(1)).count(); // NOSONAR
        verify(coll, times(0)).countDocuments();
        // new countDocuments
        coll = mockCollection(NEW_STUFF_NAMESPACE);
        verify(coll, times(0)).count(); // NOSONAR
        verify(coll, times(0)).countDocuments();
        countDocuments(coll);
        verify(coll, times(0)).count(); // NOSONAR
        verify(coll, times(1)).countDocuments();

        // old count with filter
        Bson mongoDoc = new Document();
        coll = mockCollection(OLD_STUFF_NAMESPACE);
        verify(coll, times(0)).count(mongoDoc); // NOSONAR
        verify(coll, times(0)).countDocuments(mongoDoc);
        countDocuments(coll, mongoDoc);
        verify(coll, times(1)).count(mongoDoc); // NOSONAR
        verify(coll, times(0)).countDocuments(mongoDoc);
        // new countDocuments with filter
        coll = mockCollection(NEW_STUFF_NAMESPACE);
        verify(coll, times(0)).count(mongoDoc); // NOSONAR
        verify(coll, times(0)).countDocuments(mongoDoc);
        countDocuments(coll, mongoDoc);
        verify(coll, times(0)).count(mongoDoc); // NOSONAR
        verify(coll, times(1)).countDocuments(mongoDoc);

        // old count with filter and count options
        CountOptions options = new CountOptions();
        coll = mockCollection(OLD_STUFF_NAMESPACE);
        verify(coll, times(0)).count(mongoDoc, options); // NOSONAR
        verify(coll, times(0)).countDocuments(mongoDoc, options);
        countDocuments(coll, mongoDoc, options);
        verify(coll, times(1)).count(mongoDoc, options); // NOSONAR
        verify(coll, times(0)).countDocuments(mongoDoc, options);
        // new countDocuments with filter and options
        coll = mockCollection(NEW_STUFF_NAMESPACE);
        verify(coll, times(0)).count(mongoDoc, options); // NOSONAR
        verify(coll, times(0)).countDocuments(mongoDoc, options);
        countDocuments(coll, mongoDoc, options);
        verify(coll, times(0)).count(mongoDoc, options); // NOSONAR
        verify(coll, times(1)).countDocuments(mongoDoc, options);
    }

    protected MongoCollection<Document> mockCollection(String mongoNamespace) {
        MongoNamespace nameSpace = new MongoNamespace(mongoNamespace);
        @SuppressWarnings("unchecked")
        MongoCollection<Document> coll = mock(MongoCollection.class);
        doReturn(nameSpace).when(coll).getNamespace();
        return coll;
    }
}
