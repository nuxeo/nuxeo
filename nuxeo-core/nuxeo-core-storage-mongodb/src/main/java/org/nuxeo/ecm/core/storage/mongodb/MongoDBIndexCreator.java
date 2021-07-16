/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.storage.mongodb;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.ecm.core.schema.PropertyIndexOrder;
import org.nuxeo.ecm.core.schema.PropertyIndexOrder.IndexOrder;
import org.nuxeo.ecm.core.schema.types.Schema;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * @since 11.5
 */
public class MongoDBIndexCreator {

    protected final PropertyCharacteristicHandler handler;

    protected final MongoCollection<Document> collection;

    protected Set<String> existingIndexes;

    public MongoDBIndexCreator(PropertyCharacteristicHandler handler, MongoCollection<Document> collection) {
        this.handler = handler;
        this.collection = collection;
    }

    public void createIndexes(Schema schema) {
        String prefix = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix : schema.getName();
        handler.getIndexedProperties(schema.getName())
               .stream()
               .filter(PropertyIndexOrder::isIndexNotNone)
               // convert property path to mongoDB index property
               .map(p -> p.replacePath(path -> prefix + ':' + pathToIndexKey(path)))
               // keep only the ones that don't already exist
               .filter(p -> !indexExists(p.getPath()))
               .map(p -> p.getIndexOrder() == IndexOrder.ASCENDING ? Indexes.ascending(p.getPath())
                       : Indexes.descending(p.getPath()))
               .forEach(i -> collection.createIndex(i, new IndexOptions().background(true)));
    }

    /**
     * Converts the given Nuxeo {@code path} to MongoDB identifier.
     * <p>
     * For example:
     * <ul>
     * <li>dc:title -&gt; dc:title</li>
     * <li>file:content/data -&gt; file:content.data</li>
     * <li>files:files/&#42;/data -&gt; files:files.data</li>
     * </ul>
     */
    public String pathToIndexKey(String path) {
        return path.replaceAll("/(\\*/)?", ".");
    }

    protected boolean indexExists(String path) {
        if (existingIndexes == null) {
            existingIndexes = collection.listIndexes()
                                        .map(d -> d.get("key", Document.class))
                                        .into(new ArrayList<>())
                                        .stream()
                                        // exclude compound indexes
                                        .filter(d -> d.size() == 1)
                                        .flatMap(d -> d.keySet().stream())
                                        .collect(Collectors.toSet());
        }
        return existingIndexes.contains(path);
    }
}
