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

package org.nuxeo.ecm.webapp.filemanager;

import javax.ejb.Remote;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.platform.api.ws.DocumentDescriptor;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;


/**
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas Kalogeropoulos</a>
 *
 */
@Remote
public interface FileManageActionsRemote extends StatefulBaseLifeCycle {

    String doPreEditActions(String doc_url, String actionID) throws ClientException;

    String editWithActions(String[] updateFields, String actionID) throws ClientException;

    String getDataForExternalEdit(String doc_url, String fieldName) throws ClientException;

    String[] getPostEditActions(String doc_url) throws ClientException;

    String[] getPreEditActions(String doc_url) throws ClientException;

    String createDocument(String parentUUID, String type, String[] properties) throws ClientException;

    ACE[] getDocumentACLFMWS(String uuid) throws ClientException;

    DocumentDescriptor getRootDocumentFMWS() throws ClientException;

    DocumentDescriptor[] getChildrenFMWS(String uuid) throws ClientException;

    boolean canCreateChildren(String parentUUID, String documentType) throws ClientException;

}
