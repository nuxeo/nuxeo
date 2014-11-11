/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     btatar
 *
 * $Id: ExportConstants.java 29029 2008-01-14 18:38:14Z ldoguin $
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

    public static final String SCHEMA_TAG = "schema";

    public static final String ACE_TAG = "entry";

    public static final String ACL_TAG = "acl";

    public static final String PERMISSION_ATTR = "permission";

    public static final String PRINCIPAL_ATTR = "principal";

    public static final String NAME_ATTR = "name";

    public static final String GRANT_ATTR = "grant";

    public static final String BLOB_DATA = "data";

    public static final String EXTERNAL_BLOB_URI = "uri";

    public static final String BLOB_MIME_TYPE = "mime-type";

    public static final String BLOB_ENCODING = "encoding";

    public static final String BLOB_FILENAME = "filename";

    // Constant utility class.
    private ExportConstants() {
    }

}
