/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webapp.localconfiguration;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

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

    public void toggleConfigurationForCurrentDocument(String configurationFacet) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument.hasFacet(configurationFacet)) {
            currentDocument.removeFacet(configurationFacet);
        } else {
            currentDocument.addFacet(configurationFacet);
        }
        documentManager.saveDocument(currentDocument);
        navigationContext.invalidateCurrentDocument();
        documentManager.save();

        Events.instance().raiseEvent(EventNames.LOCAL_CONFIGURATION_CHANGED, navigationContext.getCurrentDocument());
    }

    public void saveLocalConfiguration() {
        documentManager.saveDocument(navigationContext.getCurrentDocument());
        navigationContext.invalidateCurrentDocument();

        Events.instance().raiseEvent(EventNames.LOCAL_CONFIGURATION_CHANGED, navigationContext.getCurrentDocument());
        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get(LOCAL_CONFIGURATION_CHANGED_LABEL));
    }

}
