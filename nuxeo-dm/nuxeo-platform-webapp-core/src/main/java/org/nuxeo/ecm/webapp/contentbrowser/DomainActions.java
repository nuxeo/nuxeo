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

package org.nuxeo.ecm.webapp.contentbrowser;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Retrieves the domains found at a specific location and supports various
 * operations on them.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface DomainActions extends StatefulBaseLifeCycle,
        SelectDataModelListener {

    String DOMAINS_WORKING_LIST = "CURRENT_SELECTION";

    void initialize();

    @Destroy
    @Remove
    @PermitAll
    void destroy();

    /**
     * Gets the available domains for the selected contentRoot.
     */
    DocumentModelList getDomains() throws ClientException;

    /**
     * Invalidate cached domain list (Seam event listener)
     */
    void invalidateDomainList();

    /**
     * Indicates if the current user can create a domain.
     */
    Boolean getCanAddDomains() throws ClientException;

    /**
     * Gets the domains select list to be displayed in a table.
     */
    SelectDataModel getDomainsSelectModel() throws ClientException;

}
