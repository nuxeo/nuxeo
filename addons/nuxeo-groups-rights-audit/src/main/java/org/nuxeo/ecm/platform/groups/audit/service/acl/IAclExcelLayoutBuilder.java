/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;

public interface IAclExcelLayoutBuilder {

    /**
     * Analyze and render an ACL audit for the complete repository in unrestricted mode.
     */
    public void renderAudit(CoreSession session) throws ClientException;

    /**
     * Analyze and render an ACL audit for the complete document tree in unrestricted mode.
     */
    public void renderAudit(CoreSession session, DocumentModel doc) throws ClientException;

    /** Analyze and render an ACL audit for the input document and its children. */
    public void renderAudit(CoreSession session, DocumentModel doc, boolean unrestricted) throws ClientException;

    public void renderAudit(CoreSession session, final DocumentModel doc, boolean unrestricted, int timeout)
            throws ClientException;

    public IExcelBuilder getExcel();

}