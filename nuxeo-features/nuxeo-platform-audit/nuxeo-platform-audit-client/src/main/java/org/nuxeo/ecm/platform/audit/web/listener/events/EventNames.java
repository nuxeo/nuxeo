/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
     * Fired when a document selection changes (file, folder etc not workspace
     * or above).
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
     * Fired when content root selection is changed ( like workspaces, section
     * etc types ).
     */
    public static final String CONTENT_ROOT_SELECTION_CHANGED = "contentRootSelectionChanged";

    /**
     * Fired when the selected domain changes. Should be listened by components
     * interested specifically in domain selection change.
     */
    public static final String DOMAIN_SELECTION_CHANGED = "domainSelectionChanged";

    // Constant utility class.
    private EventNames() {
    }

}
