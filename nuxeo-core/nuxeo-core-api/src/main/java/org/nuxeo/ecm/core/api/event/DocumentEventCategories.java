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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentEventCategories.java 19250 2007-05-23 20:06:09Z sfermigier $
 */

package org.nuxeo.ecm.core.api.event;

/**
 * Document event categories.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class DocumentEventCategories {

    public static final String EVENT_DOCUMENT_CATEGORY = "eventDocumentCategory";

    public static final String EVENT_LIFE_CYCLE_CATEGORY = "eventLifeCycleCategory";

    /**
     * Category for events that are fired on behalf of client (outside core) code.
     */
    public static final String EVENT_CLIENT_NOTIF_CATEGORY = "clientCodeNotificationCategory";

    // Constant utility class
    private DocumentEventCategories() {
    }

}
