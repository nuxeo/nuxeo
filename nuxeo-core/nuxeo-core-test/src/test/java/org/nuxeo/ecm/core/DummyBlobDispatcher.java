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
package org.nuxeo.ecm.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobDispatcher;
import org.nuxeo.ecm.core.model.Document;

/**
 * Dummy blob dispatcher that stores video/* files in a second blob provider.
 */
public class DummyBlobDispatcher implements BlobDispatcher {

    private static final String PROVIDERS = "providers";

    private String defaultProvider;

    private String secondProvider;

    @Override
    public void initialize(Map<String, String> properties) {
        String list = properties.get(PROVIDERS);
        if (StringUtils.isBlank(list)) {
            throw new NuxeoException("empty providers");
        }
        String[] providers = list.split(" ");
        if (providers.length != 2) {
            throw new NuxeoException("needs exactly 2 providers");
        }
        defaultProvider = providers[0];
        secondProvider = providers[1];
    }

    @Override
    public Collection<String> getBlobProviderIds() {
        return Arrays.asList(defaultProvider, secondProvider);
    }

    @Override
    public String getBlobProvider(String repositoryName) {
        throw new NuxeoException("All blob keys should be prefixed in repository: " + repositoryName);
    }

    @Override
    public BlobDispatch getBlobProvider(Blob blob, Document doc) {
        String provider = blob.getMimeType().startsWith("video/") ? secondProvider : defaultProvider;
        return new BlobDispatch(provider, true);
    }

}
