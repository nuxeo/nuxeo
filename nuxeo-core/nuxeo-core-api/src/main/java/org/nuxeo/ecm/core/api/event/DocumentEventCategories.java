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
