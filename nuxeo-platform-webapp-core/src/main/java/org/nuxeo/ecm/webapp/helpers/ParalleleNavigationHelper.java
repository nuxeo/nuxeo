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

package org.nuxeo.ecm.webapp.helpers;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.ClientException;
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
    public String navigateToURL() throws ClientException {
        if (docRef == null) {
            return null;
        }
        return navigationContext.navigateToURL(docRef);
    }

    public String getCurrentDocumentFullUrl() {
        String internalURL = navigationContext.getCurrentDocumentFullUrl();
        return internalURL.replace(DocumentLocator.URL_PREFIX,
                PARALLELE_URL_PREFIX);
    }

    public String getDocumentFullUrl(DocumentRef docRef) {
        String internalURL = DocumentLocator.getFullDocumentUrl(
                navigationContext.getCurrentServerLocation(), docRef);
        return internalURL.replace(DocumentLocator.URL_PREFIX,
                PARALLELE_URL_PREFIX);
    }

}
