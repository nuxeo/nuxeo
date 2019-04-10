/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */
package org.nuxeo.ecm.user.registration;

/**
 * Simple POJO to hold document relative information
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @since 5.6
 */
public class DocumentRegistrationInfo {
    public static final String SCHEMA_NAME = "docinfo";

    public static final String DOCUMENT_ID_FIELD = SCHEMA_NAME + ":documentId";

    public static final String DOCUMENT_TITLE_FIELD = SCHEMA_NAME
            + ":documentTitle";

    public static final String DOCUMENT_RIGHT_FIELD = SCHEMA_NAME
            + ":permission";

    public static final String ACL_NAME = "local";

    protected String documentId;

    protected String permission;

    protected String documentTitle;

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
