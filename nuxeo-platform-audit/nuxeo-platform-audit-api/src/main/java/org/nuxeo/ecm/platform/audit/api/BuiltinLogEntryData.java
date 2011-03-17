/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public static final String LOG_EVENT_ID = "eventId";
    public static final String LOG_EVENT_DATE = "eventDate";
    public static final String LOG_DOC_UUID = "docUUID";
    public static final String LOG_DOC_PATH = "docPath";
    public static final String LOG_DOC_TYPE = "docType";
    public static final String LOG_PRINCIPAL_NAME = "principalName";
    public static final String LOG_COMMENT = "comment";
    public static final String LOG_CATEGORY = "category";
    public static final String LOG_DOC_LIFE_CYCLE = "docLifeCycle";

    private BuiltinLogEntryData() {
    }

}
