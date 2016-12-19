/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class RootSectionsPublicationTree extends SectionPublicationTree {

    protected DocumentModel currentDocument;

    protected RootSectionFinder rootFinder;

    protected boolean useRootSections = true;

    @Override
    public void initTree(String sid, CoreSession coreSession, Map<String, String> parameters,
            PublishedDocumentFactory factory, String configName, String title) {
        super.initTree(sid, coreSession, parameters, factory, configName, title);
        rootFinder = Framework.getLocalService(PublisherService.class).getRootSectionFinder(coreSession);
    }

    @Override
    public List<PublicationNode> getChildrenNodes() {
        if (currentDocument != null && useRootSections) {
            DocumentModelList rootSections = rootFinder.getAccessibleSectionRoots(currentDocument);
            if (rootSections.isEmpty()) {
                useRootSections = false;
                return super.getChildrenNodes();
            }
            List<PublicationNode> publicationNodes = new ArrayList<>();
            for (DocumentModel rootSection : rootSections) {
                if (isPublicationNode(rootSection)) {
                    publicationNodes.add(
                            new CoreFolderPublicationNode(rootSection, getConfigName(), sid, rootNode, factory));
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
    public PublicationNode getNodeByPath(String path) {
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
