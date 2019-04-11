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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

/**
 * Log entry builtin constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class BuiltinLogEntryData {

    public static final String LOG_ID = "id";

    public static final String LOG_CATEGORY = "category";

    public static final String LOG_COMMENT = "comment";

    public static final String LOG_EVENT_ID = "eventId";

    public static final String LOG_EVENT_DATE = "eventDate";

    public static final String LOG_EXTENDED = "extended";

    public static final String LOG_DOC_LIFE_CYCLE = "docLifeCycle";

    public static final String LOG_DOC_PATH = "docPath";

    public static final String LOG_DOC_TYPE = "docType";

    public static final String LOG_DOC_UUID = "docUUID";

    public static final String LOG_LOG_DATE = "logDate";

    public static final String LOG_PRINCIPAL_NAME = "principalName";

    public static final String LOG_REPOSITORY_ID = "repositoryId";

    private BuiltinLogEntryData() {
    }

}
