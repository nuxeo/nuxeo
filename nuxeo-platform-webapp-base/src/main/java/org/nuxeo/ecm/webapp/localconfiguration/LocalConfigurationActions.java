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

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;

import static org.jboss.seam.ScopeType.CONVERSATION;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("localConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class LocalConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected void toggleConfiguration(String configurationFacet,
            DocumentModel configurationDocument) throws ClientException {
        if (configurationDocument.hasFacet(configurationFacet)) {
            configurationDocument.removeFacet(configurationFacet);
        } else {
            configurationDocument.addFacet(configurationFacet);
        }
        documentManager.saveDocument(configurationDocument);
        navigationContext.invalidateCurrentDocument();
        documentManager.save();
    }

    public void toggleConfiguration(String configurationFacet)
            throws ClientException {
        toggleConfiguration(configurationFacet,
                navigationContext.getCurrentDocument());
    }

    public String saveLocalConfiguration() throws ClientException {
        // TODO do the save and add facet message
        documentActions.updateDocument();

        Events.instance().raiseEvent(EventNames.LOCAL_CONFIGURATION_CHANGED,
                navigationContext.getCurrentDocument());

        return null;
    }

}
