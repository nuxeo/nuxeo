/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.model.Document;

/**
 * Default blob dispatcher, that uses the repository name as the blob provider.
 *
 * @since 7.3
 */
public class DefaultBlobDispatcher implements BlobDispatcher {

    @Override
    public void initialize(Map<String, String> properties) {
    }

    @Override
    public String getBlobProvider(String repositoryName) {
        return repositoryName;
    }

    @Override
    public BlobDispatch getBlobProvider(Blob blob, Document doc) {
        return new BlobDispatch(doc.getRepositoryName(), false);
    }

}
