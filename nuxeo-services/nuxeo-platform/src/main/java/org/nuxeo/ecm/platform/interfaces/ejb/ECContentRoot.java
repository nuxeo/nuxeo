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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.interfaces.ejb;

import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Remove;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Workspace specific operations.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Remote
public interface ECContentRoot {

    /**
     * Removes the instance from the container once the client has no more
     * business with it.
     */
    @Remove
    void remove();

    /**
     * Returns the children list of the specified content root document.
     */
    List<DocumentModel> getContentRootChildren(String documentType,
            DocumentRef documentRef, CoreSession handle) throws ClientException;

    /**
     * Retrieves the content root documents associated with a specific domain.
     */
    List<DocumentModel> getContentRootDocuments(DocumentRef docRef,
            CoreSession handle) throws ClientException;

}
