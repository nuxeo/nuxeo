/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Narcis Paslaru
 *     Florent Guillaume
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.publisher.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

import javax.faces.application.FacesMessage;
import java.io.Serializable;
import java.util.List;

/**
 * This Seam bean manages the publishing tab.
 *
 * @author Narcis Paslaru
 * @author Florent Guillaume
 * @author Thierry Martins
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 * @author Thierry Delprat
 */
@Name("publishActions")
@Scope(ScopeType.CONVERSATION)
public class PublishActionsBean implements Serializable {

    private PublisherService publisherService;

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishActionsBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected String currentPublicationTreeNameForPublishing;

    protected PublicationTree currentPublicationTree;

    @Create
    public void create() {
        try {
            publisherService = Framework.getService(PublisherService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Publisher service not deployed.",
                    e);
        }
    }

    @Factory(value = "availablePublicationTrees", scope = ScopeType.EVENT)
    public List<String> getAvailablePublicationTrees() throws ClientException {
        return publisherService.getAvailablePublicationTree();
    }

    public String doPublish(PublicationNode publicationNode) throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        PublishedDocument publishedDocument = getCurrentPublicationTreeForPublishing().publish(
                currentDocument, publicationNode);
        if (publishedDocument.isPending()) {
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(
                            currentDocument.getType()));
        } else {
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(
                            currentDocument.getType()));
        }
        return null;
    }

    public void setCurrentPublicationTreeNameForPublishing(
            String currentPublicationTreeNameForPublishing) throws Exception {
        this.currentPublicationTreeNameForPublishing = currentPublicationTreeNameForPublishing;
        if (currentPublicationTree != null) {
            currentPublicationTree.release();
            currentPublicationTree = null;
        }
        currentPublicationTree = getCurrentPublicationTreeForPublishing();
    }

    public String getCurrentPublicationTreeNameForPublishing()
            throws ClientException {
        if (currentPublicationTreeNameForPublishing == null) {
            List<String> publicationTrees = getAvailablePublicationTrees();
            if (!publicationTrees.isEmpty()) {
                currentPublicationTreeNameForPublishing = publicationTrees.get(0);
            }
        }
        return currentPublicationTreeNameForPublishing;
    }

    public PublicationTree getCurrentPublicationTreeForPublishing()
            throws ClientException {
        if (currentPublicationTree == null) {
            currentPublicationTree = publisherService.getPublicationTree(
                    getCurrentPublicationTreeNameForPublishing(),
                    documentManager, null);
        }
        return currentPublicationTree;
    }

    public String getCurrentPublicationTreeIconExpanded() throws Exception {
        return getCurrentPublicationTreeForPublishing().getIconExpanded();
    }

    public String getCurrentPublicationTreeIconCollapsed() throws Exception {
        return getCurrentPublicationTreeForPublishing().getIconCollapsed();
    }

    public List<PublishedDocument> getPublishedDocuments() throws Exception {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCurrentPublicationTreeForPublishing().getExistingPublishedDocument(
                new DocumentLocationImpl(currentDocument));
    }

    public String unPublish(PublishedDocument publishedDocument)
            throws Exception {
        getCurrentPublicationTreeForPublishing().unpublish(publishedDocument);
        return null;
    }

    public boolean canPublishTo(PublicationNode publicationNode)
            throws ClientException {
        return getCurrentPublicationTreeForPublishing().canPublishTo(
                publicationNode);
    }

    public boolean canUnpublish(PublishedDocument publishedDocument)
            throws ClientException {
        return getCurrentPublicationTreeForPublishing().canUnpublish(
                publishedDocument);
    }

    public boolean isPublishedDocument() {
        return publisherService.isPublishedDocument(navigationContext.getCurrentDocument());
    }

    public boolean canManagePublishing() throws ClientException {
        PublicationTree tree = publisherService.getPublicationTreeFor(navigationContext.getCurrentDocument(), documentManager);
        return tree.ca
    }

    @Destroy
    public void destroy() {
        currentPublicationTree.release();
        currentPublicationTree = null;
    }

}
