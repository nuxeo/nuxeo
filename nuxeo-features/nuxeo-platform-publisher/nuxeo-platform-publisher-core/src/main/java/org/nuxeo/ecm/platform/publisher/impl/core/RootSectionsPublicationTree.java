/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionsFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionsFinderHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class RootSectionsPublicationTree extends SectionPublicationTree {

    protected DocumentModel currentDocument;

    protected RootSectionsFinder rootFinder;

    protected boolean useRootSections = true;

    @Override
    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
    		PublishedDocumentFactory factory, String configName, String title) throws ClientException {
        super.initTree(sid, coreSession, parameters, factory, configName, title);
        rootFinder = RootSectionsFinderHelper.getRootSectionsFinder(coreSession);
    }

    @Override
    public List<PublicationNode> getChildrenNodes() throws ClientException {
        if (currentDocument != null && useRootSections) {
            DocumentModelList rootSections = rootFinder.getAccessibleSectionRoots(currentDocument);
            if (rootSections.isEmpty()) {
                useRootSections = false;
                return super.getChildrenNodes();
            }
            List<PublicationNode> publicationNodes = new ArrayList<PublicationNode>();
            for (DocumentModel rootSection : rootSections) {
                if (isPublicationNode(rootSection)) {
                    publicationNodes.add(new CoreFolderPublicationNode(rootSection,
                            getConfigName(), sid, rootNode, factory));
                }
            }
            return publicationNodes;
        }
        return super.getChildrenNodes();
    }

    @Override
    public void setCurrentDocument(DocumentModel currentDocument) {
        this.currentDocument = currentDocument;
        rootFinder.reset();
        useRootSections = true;
    }

    @Override
    public PublicationNode getNodeByPath(String path) throws ClientException {
        if (!useRootSections) {
            return super.getNodeByPath(path);
        }
        // if we ask for the root path of this tree, returns this because
        // of the custom implementations of some methods (getChildrenNodes)
        if (path.equals(rootPath)) {
            return this;
        } else {
            // if we ask for a section root, returns a correct PublicationNode
            // (with parent set to this tree)
            List<PublicationNode> children = getChildrenNodes();
            for (PublicationNode child : children) {
                if (child.getPath().equals(path)) {
                    return child;
                }
            }
            return super.getNodeByPath(path);
        }
    }

}
