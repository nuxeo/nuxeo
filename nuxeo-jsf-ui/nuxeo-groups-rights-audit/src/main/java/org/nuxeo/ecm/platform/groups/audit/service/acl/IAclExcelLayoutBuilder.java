/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;

public interface IAclExcelLayoutBuilder {

    /**
     * Analyze and render an ACL audit for the complete repository in unrestricted mode.
     */
    void renderAudit(CoreSession session);

    /**
     * Analyze and render an ACL audit for the complete document tree in unrestricted mode.
     */
    void renderAudit(CoreSession session, DocumentModel doc);

    /** Analyze and render an ACL audit for the input document and its children. */
    void renderAudit(CoreSession session, DocumentModel doc, boolean unrestricted);

    void renderAudit(CoreSession session, final DocumentModel doc, boolean unrestricted, int timeout);

    IExcelBuilder getExcel();

}
