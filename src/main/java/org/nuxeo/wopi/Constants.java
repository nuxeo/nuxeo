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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

/**
 * @since 10.3
 */
public class Constants {

    private Constants() {
        // constants class
    }

    public static final String WOPI_LOCKS_STORE_NAME = "wopi-locks";

    public static final String WOPI_SOURCE = "wopi";

    public static final String FILE_CONTENT_PROPERTY = "file:content";

    public static final String ACCESS_TOKEN = "accessToken";

    public static final String FORM_URL = "formURL";

    public static final String WOPI_SRC = "WOPISrc";

    public static final long LOCK_TTL = 30 * 60 * 1000; // 30 minutes

    // -------- CheckFileInfo ---------------

    // -------- Required properties ---------------

    public static final String BASE_FILE_NAME = "BaseFileName";

    public static final String OWNER_ID = "OwnerId";

    public static final String SIZE = "Size";

    public static final String USER_ID = "UserId";

    public static final String VERSION = "Version";

    // -------- Host capabilities properties ---------------

    public static final String SUPPORTS_LOCKS = "SupportsLocks";

    public static final String SUPPORTS_RENAME = "SupportsRename";

    public static final String SUPPORTS_UPDATE = "SupportsUpdate";

    public static final String SUPPORTS_DELETE_FILE = "SupportsDeleteFile";

    public static final String SUPPORTED_SHARE_URL_TYPES = "SupportedShareUrlTypes";

    // -------- User metadata properties ---------------

    public static final String IS_ANONYMOUS_USER = "IsAnonymousUser";

    public static final String USER_FRIENDLY_NAME = "UserFriendlyName";

    // -------- User permissions properties ---------------

    public static final String READ_ONLY = "ReadOnly";

    public static final String USER_CAN_RENAME = "UserCanRename";

    public static final String USER_CAN_WRITE = "UserCanWrite";

    public static final String USER_CAN_NOT_WRITE_RELATIVE = "UserCanNotWriteRelative";

    // -------- End CheckFileInfo ---------------

    // -------- Rename and PutRelativeFile ---------------

    public static final String NAME = "Name";

    public static final String URL = "Url";

    public static final String HOST_VIEW_URL = "HostViewUrl";

    public static final String HOST_EDIT_URL = "HostEditUrl";

    // -------- End Rename and PutRelativeFile ---------------

    // -------- GetShareUrl ---------------

    public static final String SHARE_URL = "ShareUrl";

    public static final String SHARE_URL_READ_ONLY = "ReadOnly";

    public static final String SHARE_URL_READ_WRITE = "ReadWrite";

    // -------- End GetShareUrl ---------------

}
