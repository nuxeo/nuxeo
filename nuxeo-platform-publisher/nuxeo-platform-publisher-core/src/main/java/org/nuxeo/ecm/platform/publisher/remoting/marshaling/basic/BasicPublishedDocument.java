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

    public BasicPublishedDocument(DocumentRef docRef, String repositoryName,
            String serverName, String versionLabel, String path,
            String parentPath, boolean isPending) {
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
