/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;

/**
 * Java implementation for the marshalled {@link PublishedDocument}.
 *
 * @author tiry
 */
public class BasicPublishedDocument implements PublishedDocument {

    private DocumentRef docRef;

    private String repositoryName;

    private String serverName;

    private String versionLabel;

    private String path;

    private String parentPath;

    private boolean isPending;

    private static final long serialVersionUID = 1L;

    public BasicPublishedDocument(DocumentRef docRef, String repositoryName, String serverName, String versionLabel,
            String path, String parentPath, boolean isPending) {
        this.docRef = docRef;
        this.repositoryName = repositoryName;
        this.serverName = serverName;
        this.versionLabel = versionLabel;
        this.path = path;
        this.parentPath = parentPath;
        this.isPending = isPending;
    }

    public DocumentRef getSourceDocumentRef() {
        return docRef;
    }

    public String getSourceRepositoryName() {
        return repositoryName;
    }

    public String getSourceServer() {
        return serverName;
    }

    public String getSourceVersionLabel() {
        return versionLabel;
    }

    public String getPath() {
        return path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public boolean isPending() {
        return isPending;
    }

    public Type getType() {
        return Type.REMOTE;
    }

}
