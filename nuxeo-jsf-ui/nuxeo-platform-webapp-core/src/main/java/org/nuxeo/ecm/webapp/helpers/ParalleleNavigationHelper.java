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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.DocumentLocator;

@Startup
@Name("paralleleNavigationHelper")
@Scope(SESSION)
public class ParalleleNavigationHelper implements Serializable {

    private static final long serialVersionUID = 16794309876876L;

    public static final String PARALLELE_URL_PREFIX = "/parallele.faces?";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    ConversationIdGenerator conversationIdGenerator;

    @RequestParameter
    String docRef;

    // Start a new Main conversation
    @Begin(id = "#{conversationIdGenerator.nextMainConversationId}")
    public String navigateToURL() {
        if (docRef == null) {
            return null;
        }
        return navigationContext.navigateToURL(docRef);
    }

    public String getCurrentDocumentFullUrl() {
        String internalURL = navigationContext.getCurrentDocumentFullUrl();
        return internalURL.replace(DocumentLocator.URL_PREFIX, PARALLELE_URL_PREFIX);
    }

    public String getDocumentFullUrl(DocumentRef docRef) {
        String internalURL = DocumentLocator.getFullDocumentUrl(navigationContext.getCurrentServerLocation(), docRef);
        return internalURL.replace(DocumentLocator.URL_PREFIX, PARALLELE_URL_PREFIX);
    }

}
