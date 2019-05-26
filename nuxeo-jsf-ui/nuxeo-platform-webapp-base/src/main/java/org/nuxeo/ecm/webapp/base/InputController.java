/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.webapp.base;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.rest.FancyNavigationHandler;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Contains generic functionality usable by all action listeners.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public abstract class InputController {

    private static final Log log = LogFactory.getLog(InputController.class);

    @In(create = true)
    protected ActionManager actionManager;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected EventManager eventManager;

    @In(required = false, create = true)
    /**
     * @deprecated Since 5.2. Injecting current document is not a good idea, should be fetched from navigationContext directly.
     */
    @Deprecated
    protected DocumentModel currentDocument;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    // won't inject this because of seam problem after activation
    // ::protected Map<String, String> messages;
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected TypesTool typesTool;

    @In(create = true, required = false)
    protected NuxeoPrincipal currentUser;

    /**
     * Utility method that helps remove a {@link DocumentModel} from a list. The document models are compared on
     * {@link DocumentRef}s.
     *
     * @param documentList
     * @param document
     */
    public void removeDocumentFromList(List<DocumentModel> documentList, DocumentModel document) {
        if (null == document) {
            log.error("Received nul doc, not removing anything...");
            return;
        }

        log.debug("Removing document " + document.getId() + " from list...");

        for (int i = 0; i < documentList.size(); i++) {
            if (documentList.get(i).getRef().equals(document.getRef())) {
                documentList.remove(i);
            }
        }
    }

    /**
     * Logs a {@link DocumentModel} title and the passed string (info).
     */
    public void logDocumentWithTitle(String someLogString, DocumentModel document) {
        if (null != document) {
            log.trace('[' + getClass().getSimpleName() + "] " + someLogString + ' ' + document.getId());
            log.debug("CURRENT DOC PATH: " + document.getPathAsString());
        } else {
            log.trace('[' + getClass().getSimpleName() + "] " + someLogString + " NULL DOC");
        }
    }

    /**
     * Logs a {@link DocumentModel} name and the passed string (info).
     */
    public void logDocumentWithName(String someLogString, DocumentModel document) {
        if (null != document) {
            log.debug('[' + getClass().getSimpleName() + "] " + someLogString + ' ' + document.getName());
        } else {
            log.debug('[' + getClass().getSimpleName() + "] " + someLogString + " NULL DOC");
        }
    }

    /**
     * Extracts references from a list of document models.
     */
    protected List<DocumentRef> extractReferences(List<DocumentModel> documents) {
        List<DocumentRef> references = new ArrayList<>();

        for (DocumentModel docModel : documents) {
            references.add(docModel.getRef());
        }

        return references;
    }

    protected void setFacesMessage(String msg) {
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(msg));
    }

    /**
     * Is the current logged user an administrator?
     */
    public boolean getAdministrator() {
        return currentUser.isAdministrator();
    }

    /**
     * Returns null.
     * <p>
     * Previous behavior was: Utility method to return non 'null' JSF outcome that do not change the current view. The
     * problem with null outcome is that some seam components are not refetched and thus the JSF tree might hold
     * references that are no longer up-to-date, esp. in search results views whose documents lists are computed by an
     * EVENT scoped seam factory.
     *
     * @param actionOutcome a string that might be used in the future to compute the JSF outcome in a cleaner way
     * @return the same view as previously based on the expectation that the 'outcome_name' match the view id
     *         '/outcome_name.xhtml' faces-config.xml
     * @deprecated returning a non-null outcome is now useless since our {@link FancyNavigationHandler} already performs
     *             redirection to the right outcome when dealing with a null outcome. Plus assumptions on the
     *             view/outcome names here was a buggy hack.
     */
    @Deprecated
    public String computeOutcome(String actionOutcome) {
        // actionOutcome is currently ignored on purpose but might be useful in
        // the future
        return null;
    }

}
