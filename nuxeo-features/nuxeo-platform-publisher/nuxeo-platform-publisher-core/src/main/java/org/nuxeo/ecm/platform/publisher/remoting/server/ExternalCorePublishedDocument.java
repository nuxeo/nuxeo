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

package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.ExtendedDocumentLocation;

/**
 * {@link PublishedDocument} implementation that uses a {@link DocumentModel} to store the result of a remote
 * publication.
 *
 * @author tiry
 */
public class ExternalCorePublishedDocument implements PublishedDocument {

    private static final long serialVersionUID = 1L;

    protected String sourceServer;

    protected String repositoryName;

    protected DocumentRef ref;

    protected String versionLabel;

    protected String path;

    protected String parentPath;

    protected boolean isPending;

    public ExternalCorePublishedDocument(DocumentModel doc) {
        ExtendedDocumentLocation xLoc = ExtendedDocumentLocation.extractFromDoc(doc);
        this.sourceServer = xLoc.getOriginalServer();
        this.repositoryName = xLoc.getServerName();
        this.ref = xLoc.getDocRef();
        versionLabel = VersioningHelper.getVersionLabelFor(doc);
        Path p = doc.getPath();
        path = p.toString();
        parentPath = p.removeLastSegments(1).toString();
    }

    public DocumentRef getSourceDocumentRef() {
        return ref;
    }

    public String getSourceRepositoryName() {
        return repositoryName;
    }

    public String getSourceServer() {
        return sourceServer;
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
