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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.el.ContextStringWrapper;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Provides contentRoot-specific actions.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
// XXX AT: content roots should be handled like documents => deprecated, use
public interface WorkspaceActions extends StatefulBaseLifeCycle {

    void initialize();

    @Destroy
    @Remove
    @PermitAll
    void destroy();

    boolean getAdministrator();

    DocumentModelList getTemplates() throws ClientException;

    DocumentModel getTmpWorkspace();

    String finishPageFlow();

    void setUseTemplate(Boolean value);

    Boolean getUseTemplate();

    ContextStringWrapper FactoryForSelectedTemplateId();

    ContextStringWrapper FactoryForSelectSecurityModel();

    ContextStringWrapper FactoryForSelectSecurityOwner();

    String getSelectedTemplateDescription();

    DocumentModel getSelectedTemplate();

    String createWorkspace() throws ClientException;

    String exitWizard() throws ClientException;

    String getA4JHackingURL();

}
