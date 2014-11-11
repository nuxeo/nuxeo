/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.ExtendedDocumentLocation;

/**
 * {@link PublishedDocument} implementation that uses a {@link DocumentModel} to
 * store the result of a remote publication.
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

    public ExternalCorePublishedDocument(DocumentModel doc)
            throws ClientException {
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
