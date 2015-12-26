/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.publisher.impl.core;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

public class EmptyRoot extends CoreFolderPublicationNode {

    private static final long serialVersionUID = 1L;

    public EmptyRoot(String treeConfigName, String sid, PublishedDocumentFactory factory) {
        super(null, treeConfigName, sid, factory);
    }

    @SuppressWarnings("unchecked")
    public List<PublishedDocument> getChildrenDocuments() {
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<PublicationNode> getChildrenNodes() {
        return Collections.emptyList();
    }

    @Override
    public String getPath() {
        return "/";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getTitle() {
        return "";
    }

}
