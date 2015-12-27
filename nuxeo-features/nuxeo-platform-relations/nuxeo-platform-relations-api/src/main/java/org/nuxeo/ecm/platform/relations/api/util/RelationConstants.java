/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Eugen Ionica
 */

package org.nuxeo.ecm.platform.relations.api.util;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

/**
 * Constants for relations management.
 *
 * @author Anahide Tchertchian
 * @author Eugen Ionica
 */
public class RelationConstants {

    public static final String GRAPH_NAME = "default";

    public static final String METADATA_NAMESPACE = "http://www.nuxeo.org/metadata/";

    public static final String DOCUMENT_NAMESPACE = "http://www.nuxeo.org/document/uid/";

    // statement metadata

    public static final Resource TITLE = new ResourceImpl(METADATA_NAMESPACE + "title");

    public static final Resource UUID = new ResourceImpl(METADATA_NAMESPACE + "uuid");

    public static final Resource CREATION_DATE = new ResourceImpl(METADATA_NAMESPACE + "CreationDate");

    public static final Resource MODIFICATION_DATE = new ResourceImpl(METADATA_NAMESPACE + "ModificationDate");

    public static final Resource AUTHOR = new ResourceImpl(METADATA_NAMESPACE + "Author");

    // XXX AT: for BBB, use a different namespace for comment
    public static final Resource COMMENT = new ResourceImpl("http://www.nuxeo.org/comment");

    public static final Resource COPY_FROM_WORK_VERSION = new ResourceImpl(METADATA_NAMESPACE
            + "copy-from-work-version");

    // Constant utility class
    private RelationConstants() {
    }

}
