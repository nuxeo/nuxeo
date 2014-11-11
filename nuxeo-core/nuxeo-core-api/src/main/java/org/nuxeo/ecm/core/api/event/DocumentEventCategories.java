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
