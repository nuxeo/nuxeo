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
     * The key in the info map pointing to the frozen document (previous
     * version).
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
