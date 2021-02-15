/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     btatar
 *
 */

package org.nuxeo.ecm.core.io;

/**
 * Constants that provide the types for which the reader are for.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
public final class ExportConstants {

    // All these constants are actually used. This is Good.

    public static final String ZIP_HEADER = "=========== Nuxeo ECM Archive v. 1.0.0 ===========\r\n";

    public static final String MARKER_FILE = ".nuxeo-archive";

    public static final String DOCUMENT_FILE = "document.xml";

    public static final String DOCUMENT_TAG = "document";

    public static final String SYSTEM_TAG = "system";

    public static final String REP_NAME = "repository";

    public static final String ID_ATTR = "id";

    public static final String PATH_TAG = "path";

    public static final String TYPE_TAG = "type";

    public static final String LIFECYCLE_STATE_TAG = "lifecycle-state";

    public static final String LIFECYCLE_POLICY_TAG = "lifecycle-policy";

    public static final String ACCESS_CONTROL_TAG = "access-control";

    public static final String FACET_TAG = "facet";

    public static final String SCHEMA_TAG = "schema";

    public static final String ACE_TAG = "entry";

    public static final String ACL_TAG = "acl";

    public static final String PERMISSION_ATTR = "permission";

    public static final String PRINCIPAL_ATTR = "principal";

    public static final String NAME_ATTR = "name";

    public static final String GRANT_ATTR = "grant";

    public static final String CREATOR_ATTR = "creator";

    public static final String BEGIN_ATTR = "begin";

    public static final String END_ATTR = "end";

    public static final String BLOB_DATA = "data";

    public static final String EXTERNAL_BLOB_URI = "uri";

    public static final String BLOB_MIME_TYPE = "mime-type";

    public static final String BLOB_ENCODING = "encoding";

    public static final String BLOB_FILENAME = "filename";

    public static final String BLOB_DIGEST = "digest";

    // Constant utility class.
    private ExportConstants() {
    }

}
