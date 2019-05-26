/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: EventNames.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.web.listener.events;

/**
 * Seam event identifiers.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
// :FIXME: duplicated from the Nuxeo project. Use directly the ones there once
// the WDK defined.
public class EventNames {

    /**
     * Fired when a document selection changes (file, folder etc not workspace or above).
     */
    public static final String DOCUMENT_SELECTION_CHANGED = "documentSelectionChanged";

    /**
     * Fired when a location selection changes.
     */
    public static final String LOCATION_SELECTION_CHANGED = "locationSelectionChanged";

    /**
     * Should be raised when a document is edited.
     */
    public static final String DOCUMENT_CHANGED = "documentChanged";

    /**
     * Fired when content root selection is changed ( like workspaces, section etc types ).
     */
    public static final String CONTENT_ROOT_SELECTION_CHANGED = "contentRootSelectionChanged";

    /**
     * Fired when the selected domain changes. Should be listened by components interested specifically in domain
     * selection change.
     */
    public static final String DOMAIN_SELECTION_CHANGED = "domainSelectionChanged";

    // Constant utility class.
    private EventNames() {
    }

}
