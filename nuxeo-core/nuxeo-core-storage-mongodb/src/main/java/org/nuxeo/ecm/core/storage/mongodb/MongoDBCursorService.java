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

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;

import org.bson.Document;
import org.nuxeo.ecm.core.api.CursorService;

import com.mongodb.client.MongoCursor;

/**
 * MongoDB implementation of the {@link CursorService}.
 * <p>
 * (Extracted as a separate class to spare the developer from the complex generics.)
 *
 * @since 11.1
 */
public class MongoDBCursorService extends CursorService<MongoCursor<Document>, Document, String> {

    public MongoDBCursorService(MongoDBConverter converter) {
        super(ob -> (String) ob.get(converter.keyToBson(KEY_ID)));
    }

}
