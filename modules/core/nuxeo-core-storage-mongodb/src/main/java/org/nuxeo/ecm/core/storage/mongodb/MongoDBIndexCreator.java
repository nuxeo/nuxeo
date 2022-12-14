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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.ecm.core.schema.PropertyIndexOrder;
import org.nuxeo.ecm.core.schema.PropertyIndexOrder.IndexOrder;
import org.nuxeo.ecm.core.schema.types.Schema;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * @since 2021.8
 */
public class MongoDBIndexCreator {

    private static final Logger log = LogManager.getLogger(MongoDBIndexCreator.class);

    protected final PropertyCharacteristicHandler handler;

    protected final MongoCollection<Document> collection;

    protected Map<String, Document> existingIndexes;

    public MongoDBIndexCreator(PropertyCharacteristicHandler handler, MongoCollection<Document> collection) {
        this.handler = handler;
        this.collection = collection;
    }

    public void createIndexes(Schema schema) {
        var indexes = handler.getIndexedProperties(schema.getName())
                             .stream()
                             .filter(PropertyIndexOrder::isIndexNotNone)
                             // convert property path to mongoDB index property
                             .map(p -> p.replacePath(path -> pathToIndexKey(schema, path)))
                             .map(this::toIndexModel)
                             .collect(toList());
        createIndexes(indexes);
    }

    public void createIndexes(List<IndexModel> indexes) {
        var existingIndexes = getExistingIndexes();
        var toCreate = new ArrayList<IndexModel>();
        for (var index : indexes) {
            String indexKey = getIndexName(index);
            if (!existingIndexes.containsKey(indexKey)) {
                log.info("The index: {} is about to be created", indexKey);
                toCreate.add(index);
            } else if (!hasCorrectDefinition(index, existingIndexes.get(indexKey))) {
                log.warn("The index: {} has not the correct definition, please check you DB, expected: {}, actual:{}",
                        indexKey, index, existingIndexes.get(indexKey));
            } else {
                log.info("The index: {} is already configured", indexKey);
            }
        }
        if (!toCreate.isEmpty()) {
            collection.createIndexes(toCreate);
        }
    }

    /**
     * Converts the given Nuxeo {@code path} to MongoDB identifier by taking into account the schema prefix.
     * 
     * @since 2023.0
     */
    protected String pathToIndexKey(Schema schema, String path) {
        var prefix = schema.getNamespace().hasPrefix() ? schema.getNamespace().prefix : schema.getName();
        return prefix + ':' + pathToIndexKey(path);
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
    protected String pathToIndexKey(String path) {
        return path.replaceAll("/(\\*/)?", ".");
    }

    protected IndexModel toIndexModel(PropertyIndexOrder property) {
        var key = property.getIndexOrder() == IndexOrder.ASCENDING ? Indexes.ascending(property.getPath())
                : Indexes.descending(property.getPath());
        return new IndexModel(key, new IndexOptions().background(true));
    }

    protected Map<String, Document> getExistingIndexes() {
        if (existingIndexes == null) {
            existingIndexes = collection.listIndexes()
                                        .into(new ArrayList<>())
                                        .stream()
                                        .collect(toMap(d -> d.get("name", String.class), Function.identity()));
        }
        return existingIndexes;
    }

    protected String getIndexName(IndexModel index) {
        if (StringUtils.isNotBlank(index.getOptions().getName())) {
            return index.getOptions().getName();
        }
        var builder = new StringBuilder();
        for (var entry : ((BsonDocument) index.getKeys()).entrySet()) {
            if (builder.length() > 0) {
                builder.append('_');
            }
            builder.append(entry.getKey()).append('_');
            // just two cases in index case
            if (entry.getValue().isNumber()) {
                builder.append(entry.getValue().asNumber().intValue());
            } else if (entry.getValue().isString()) {
                builder.append(entry.getValue().asString().getValue());
            }
        }
        return builder.toString();
    }

    protected boolean hasCorrectDefinition(IndexModel index, Document actualIndex) {
        var options = index.getOptions();
        // check basic options, excluding version fields and specific options such as 2d/geo ones
        boolean result = checkDefinition(options.isUnique(), actualIndex, "unique");
        result = result && checkDefinition(options.isSparse(), actualIndex, "sparse");
        result = result && checkDefinition(options.getExpireAfter(TimeUnit.SECONDS), actualIndex, "expireAfterSeconds");
        result = result && checkDefinition(options.getLanguageOverride(), actualIndex, "language_override");
        result = result
                && checkDefinition(options.getPartialFilterExpression(), actualIndex, "partialFilterExpression");
        result = result && checkDefinition(options.isHidden(), actualIndex, "hidden");
        return result;
    }

    protected boolean checkDefinition(boolean expectedValue, Document actualIndex, String fieldName) {
        return !expectedValue || actualIndex.getBoolean(fieldName, false);
    }

    protected boolean checkDefinition(Object expectedValue, Document actualIndex, String fieldName) {
        return Objects.equals(expectedValue, actualIndex.get(fieldName));
    }

    protected boolean checkDefinition(Bson expectedValue, Document actualIndex, String fieldName) {
        var actualValue = actualIndex.get(fieldName, Document.class);
        if (expectedValue == null || actualValue == null) {
            return expectedValue == null && actualValue == null;
        }
        var expectedDoc = expectedValue.toBsonDocument(BsonDocument.class, collection.getCodecRegistry());
        var actualDoc = actualValue.toBsonDocument(BsonDocument.class, collection.getCodecRegistry());
        return expectedDoc.equals(actualDoc);
    }
}
