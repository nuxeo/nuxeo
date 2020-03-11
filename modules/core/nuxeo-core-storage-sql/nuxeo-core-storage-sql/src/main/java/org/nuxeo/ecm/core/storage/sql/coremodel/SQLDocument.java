/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * Base interface for SQL documents.
 */
public interface SQLDocument extends Document {

    String SIMPLE_TEXT_SYS_PROP = "fulltextSimple";

    String BINARY_TEXT_SYS_PROP = "fulltextBinary";

    String FULLTEXT_JOBID_SYS_PROP = "fulltextJobId";

    String IS_TRASHED_SYS_PROP = "isTrashed";

    /**
     * Returns the node with info about the hierarchy location.
     */
    Node getNode();

}
