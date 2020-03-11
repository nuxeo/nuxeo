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
 * $Id$
 */

package org.nuxeo.ecm.core.api;

/**
 * Helper class to send versions change event notifications in the core.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public final class VersioningChangeNotifier {

    /**
     * Core event id for events dispatched by this class.
     */
    public static final String CORE_EVENT_ID_VERSIONING_CHANGE = "versioningChangeCoreEvent";

    /**
     * The key in the info map pointing to the frozen document (previous version).
     */
    public static final String EVT_INFO_OLD_DOC_KEY = "oldDoc";

    /**
     * The key in the info map pointing to the current document.
     */
    public static final String EVT_INFO_NEW_DOC_KEY = "newDoc";

    // Utility class.
    private VersioningChangeNotifier() {
    }

}
