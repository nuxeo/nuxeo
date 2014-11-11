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

package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;

/**
 * Implementations of the {@link PublishedDocument} on top of the Core, using
 * simple proxies.
 *
 * @author tiry
 */
public class SimpleCorePublishedDocument implements PublishedDocument {

    private static final long serialVersionUID = 1L;

    protected DocumentModel proxy;

    protected String versionLabel;

    protected boolean isPending;

    public SimpleCorePublishedDocument(DocumentModel doc)
            throws ClientException {
        if (!doc.isProxy()) {
            throw new ClientException("DocumentModel is not a proxy");
        }
        this.proxy = doc;
        this.versionLabel = VersioningHelper.getVersionLabelFor(doc);
    }

    public DocumentRef getSourceDocumentRef() {
        return new IdRef(proxy.getSourceId());
    }

    public String getSourceServer() {
        return "local";
    }

    public String getSourceRepositoryName() {
        return proxy.getRepositoryName();
    }

    public String getSourceVersionLabel() {
        return versionLabel;
    }

    public DocumentModel getProxy() {
        return proxy;
    }

    public String getPath() {
        return proxy.getPathAsString();
    }

    public String getParentPath() {
        Path path = proxy.getPath();
        return path.removeLastSegments(1).toString();
    }

    public void setPending(boolean isPending) {
        this.isPending = isPending;
    }

    public boolean isPending() {
        return isPending;
    }

    public Type getType() {
        return Type.LOCAL;
    }

}
