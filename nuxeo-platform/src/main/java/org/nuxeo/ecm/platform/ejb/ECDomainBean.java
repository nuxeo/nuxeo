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

package org.nuxeo.ecm.platform.ejb;

import java.util.List;

import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.interfaces.local.ECDomainLocal;

/**
 * Domain implementation.
 *
 * @author Razvan Caraghin
 */
@Stateless
public class ECDomainBean implements ECDomainLocal {

    public List<DocumentModel> getDomains(CoreSession handle)
            throws ClientException {
        try {
            assert null != handle;
            // assert(handle.getSessionId());
            DocumentRef rootRef = handle.getRootDocument().getRef();
            assert null != rootRef;

            if (!handle.hasPermission(rootRef, SecurityConstants.READ_CHILDREN)) {
                return new DocumentModelListImpl();
            }

            return handle.getChildren(rootRef);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void remove() {
    }

}
