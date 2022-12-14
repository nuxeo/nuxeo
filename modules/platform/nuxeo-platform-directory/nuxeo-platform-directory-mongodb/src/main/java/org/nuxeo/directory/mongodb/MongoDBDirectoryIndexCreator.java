/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.directory.mongodb;

import org.bson.Document;
import org.nuxeo.ecm.core.schema.PropertyCharacteristicHandler;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBIndexCreator;

import com.mongodb.client.MongoCollection;

/**
 * @since 2023.0
 */
public class MongoDBDirectoryIndexCreator extends MongoDBIndexCreator {

    public MongoDBDirectoryIndexCreator(PropertyCharacteristicHandler handler, MongoCollection<Document> collection) {
        super(handler, collection);
    }

    @Override
    protected String pathToIndexKey(Schema schema, String path) {
        // MongoDB directories don't handle the schema prefix like the repository does, prefix is written to the MongoDB
        // document if and only if it is declared in the schema contribution
        if (schema.getNamespace().hasPrefix()) {
            return schema.getNamespace().prefix + ':' + pathToIndexKey(path);
        }
        return pathToIndexKey(path);
    }
}
