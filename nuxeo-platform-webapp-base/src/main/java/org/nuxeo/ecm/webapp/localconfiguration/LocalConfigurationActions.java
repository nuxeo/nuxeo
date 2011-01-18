/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webapp.localconfiguration;

import java.io.Serializable;

import javax.faces.application.FacesMessage;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

import static org.jboss.seam.ScopeType.CONVERSATION;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("localConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class LocalConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LOCAL_CONFIGURATION_CHANGED_LABEL = "label.local.configuration.modified";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    public void toggleConfigurationForCurrentDocument(String configurationFacet) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument.hasFacet(configurationFacet)) {
            currentDocument.removeFacet(configurationFacet);
        } else {
            currentDocument.addFacet(configurationFacet);
        }
        documentManager.saveDocument(currentDocument);
        navigationContext.invalidateCurrentDocument();
        documentManager.save();

        Events.instance().raiseEvent(EventNames.LOCAL_CONFIGURATION_CHANGED,
                navigationContext.getCurrentDocument());
    }

    public void saveLocalConfiguration() throws ClientException {
        documentManager.saveDocument(navigationContext.getCurrentDocument());
        navigationContext.invalidateCurrentDocument();

        Events.instance().raiseEvent(EventNames.LOCAL_CONFIGURATION_CHANGED,
                navigationContext.getCurrentDocument());
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(LOCAL_CONFIGURATION_CHANGED_LABEL));
    }

}
