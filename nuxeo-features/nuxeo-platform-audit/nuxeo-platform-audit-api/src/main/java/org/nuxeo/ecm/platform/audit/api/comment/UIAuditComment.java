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
package org.nuxeo.ecm.platform.audit.api.comment;

/**
 * Simple POJO to store pre-processed comment and associated document.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class UIAuditComment {

    protected final String comment;

    protected final LinkedDocument linkedDoc;

    public UIAuditComment(String comment, LinkedDocument linkedDoc) {
        this.comment = comment;
        this.linkedDoc = linkedDoc;
    }

    public String getComment() {
        return comment;
    }

    public LinkedDocument getLinkedDoc() {
        return linkedDoc;
    }

}
