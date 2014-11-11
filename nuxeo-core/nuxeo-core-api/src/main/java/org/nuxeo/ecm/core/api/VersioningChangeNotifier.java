/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
