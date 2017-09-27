/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

/**
 * TODO: move this class in order to be used by elasticsearch audit
 *
 * @since 9.1
 */
public final class LogEntryConstants {

    public static final String PROPERTY_CATEGORY = "category";

    public static final String PROPERTY_COMMENT = "comment";

    public static final String PROPERTY_DOC_LIFE_CYCLE = "docLifeCycle";

    public static final String PROPERTY_DOC_PATH = "docPath";

    public static final String PROPERTY_DOC_TYPE = "docType";

    public static final String PROPERTY_DOC_UUID = "docUUID";

    public static final String PROPERTY_EVENT_DATE = "eventDate";

    public static final String PROPERTY_EVENT_ID = "eventId";

    public static final String PROPERTY_EXTENDED = "extended";

    public static final String PROPERTY_LOG_DATE = "logDate";

    public static final String PROPERTY_PRINCIPAL_NAME = "principalName";

    public static final String PROPERTY_REPOSITORY_ID = "repositoryId";

    private LogEntryConstants() {
       // nothing
    }

}
