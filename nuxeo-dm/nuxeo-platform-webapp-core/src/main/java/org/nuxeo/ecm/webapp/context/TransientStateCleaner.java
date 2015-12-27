/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;

/**
 * Seam Bean that is responsible from refetching the CurrentDocument in case it is Dirty (Transient modifications not
 * pushed inside the DB).
 * <p>
 * This can happen if a low level Listener decides to RollBack the transaction.
 * <p>
 * In this case the DocumentModel won't be saved to the DB, but since the ApplyModelValue JSF phase has run, the
 * DocumentModel will have been modified resulting in a Dirty state inside the Context.
 * <p>
 * We want to keep this dirty state as long as we stay on the same Tab, but as soon as we navigate somewhere else, we
 * must reset the state.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
@Name("transientStateCleaner")
@Scope(ScopeType.EVENT)
@Install(precedence = FRAMEWORK)
public class TransientStateCleaner implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    protected static final Log log = LogFactory.getLog(TransientStateCleaner.class);

    @Observer(value = { WebActions.CURRENT_TAB_CHANGED_EVENT }, create = true)
    public void flushTransientStateIfNeeded() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null && currentDocument.isDirty()) {
            CoreSession documentManager = (CoreSession) Component.getInstance("documentManager", false);
            if (documentManager != null) {
                try {
                    // refetch
                    currentDocument = documentManager.getDocument(currentDocument.getRef());
                    // force refresh
                    navigationContext.setCurrentDocument(null);
                    navigationContext.setCurrentDocument(currentDocument);
                } catch (DocumentNotFoundException e) {
                    log.error("Error during reset of transient state", e);
                }
            }
        }
    }
}
