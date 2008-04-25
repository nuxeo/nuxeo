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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.util.ECInvalidParameterException;

/**
 * Domain-specific operations. Usually one can obtain workspaces using it.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Remote
public interface ECDomain {
    @Deprecated
    void remove();

    /**
     * Returns the {@link DocumentModel}s available in the passed
     * {@link RepositoryLocation} that are domains.
     *
     * @return
     * @throws ECInvalidParameterException
     * @throws ClientException
     * @throws NoSuchRepositoryException
     * @throws DocumentException
     */
    List<DocumentModel> getDomains(CoreSession handle)
            throws ECInvalidParameterException, ClientException;
}
