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

package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;

/**
 * Implementations of the {@link PublishedDocument} on top of the Core, using simple proxies.
 *
 * @author tiry
 */
public class SimpleCorePublishedDocument implements PublishedDocument {

    private static final long serialVersionUID = 1L;

    protected DocumentModel proxy;

    protected String versionLabel;

    protected boolean isPending;

    public SimpleCorePublishedDocument(DocumentModel doc) {
        if (!doc.isProxy()) {
            throw new NuxeoException("DocumentModel is not a proxy");
        }
        this.proxy = doc;
        this.versionLabel = VersioningHelper.getVersionLabelFor(doc);
    }

    @Override
    public DocumentRef getSourceDocumentRef() {
        return new IdRef(proxy.getSourceId());
    }

    @Override
    public String getSourceRepositoryName() {
        return proxy.getRepositoryName();
    }

    @Override
    public String getSourceVersionLabel() {
        return versionLabel;
    }

    public DocumentModel getProxy() {
        return proxy;
    }

    @Override
    public String getPath() {
        return proxy.getPathAsString();
    }

    @Override
    public String getParentPath() {
        Path path = proxy.getPath();
        return path.removeLastSegments(1).toString();
    }

    public void setPending(boolean isPending) {
        this.isPending = isPending;
    }

    @Override
    public boolean isPending() {
        return isPending;
    }

}
