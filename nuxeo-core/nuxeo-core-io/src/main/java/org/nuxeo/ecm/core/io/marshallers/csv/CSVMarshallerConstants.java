/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.io.marshallers.csv;

/**
 * @since 10.3
 */
public class CSVMarshallerConstants {

    // Header fields

    public static final String REPOSITORY_FIELD = "repository";

    public static final String UID_FIELD = "uid";

    public static final String PATH_FIELD = "path";

    public static final String TYPE_FIELD = "type";

    public static final String STATE_FIELD = "state";

    public static final String PARENT_REF_FIELD = "parentRef";

    public static final String IS_CHECKED_OUT_FIELD = "isCheckedOut";

    public static final String IS_VERSION_FIELD = "isVersion";

    public static final String IS_PROXY_FIELD = "isProxy";

    public static final String PROXY_TARGET_ID_FIELD = "proxyTargetId";

    public static final String VERSIONABLE_ID_FIELD = "versionableId";

    public static final String CHANGE_TOKEN_FIELD = "changeToken";

    public static final String IS_TRASHED_FIELD = "isTrashed";

    public static final String TITLE_FIELD = "title";

    public static final String VERSION_LABEL_FIELD = "versionLabel";

    public static final String LOCK_OWNER_FIELD = "lockOwner";

    public static final String LOCK_CREATED_FIELD = "lockCreated";

    public static final String LAST_MODIFIED_FIELD = "lastModified";

    private CSVMarshallerConstants() {
        // utility class
    }
}
