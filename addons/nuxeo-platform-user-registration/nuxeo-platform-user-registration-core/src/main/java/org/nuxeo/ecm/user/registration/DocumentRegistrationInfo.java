/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.registration;

import java.util.Date;

/**
 * Simple POJO to hold document relative information
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @since 5.6
 */
public class DocumentRegistrationInfo {
    public static final String SCHEMA_NAME = "docinfo";

    public static final String DOCUMENT_ID_FIELD = SCHEMA_NAME + ":documentId";

    public static final String DOCUMENT_TITLE_FIELD = SCHEMA_NAME + ":documentTitle";

    public static final String DOCUMENT_RIGHT_FIELD = SCHEMA_NAME + ":permission";

    public static final String DOCUMENT_BEGIN_FIELD = SCHEMA_NAME + ":begin";

    public static final String DOCUMENT_END_FIELD = SCHEMA_NAME + ":end";

    public static final String ACL_NAME = "local";

    protected String documentId;

    protected String permission;

    protected String documentTitle;

    /**
     * @since 7.4
     */
    protected Date begin;

    /**
     * @since 7.4
     */
    protected Date end;

    public Date getBegin() {
        return begin;
    }

    public void setBegin(Date begin) {
        this.begin = begin;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

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
