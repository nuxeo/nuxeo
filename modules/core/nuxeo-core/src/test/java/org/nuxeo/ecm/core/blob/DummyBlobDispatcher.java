/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.model.Document;

/**
 * Dummy blob dispatcher that stores video/* files in a second blob provider.
 */
public class DummyBlobDispatcher implements BlobDispatcher {

    private static final String PROVIDERS = "providers";

    protected String defaultProvider;

    protected String secondProvider;

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
    public BlobDispatch getBlobProvider(Document doc, Blob blob, String xpath) {
        String provider = blob.getMimeType().startsWith("video/") ? secondProvider : defaultProvider;
        return new BlobDispatch(provider, true);
    }

    @Override
    public void notifyChanges(Document doc, Set<String> xpaths) {
    }

    @Override
    public void notifyMakeRecord(Document doc) {
    }

    @Override
    public void notifyAfterCopy(Document doc) {
    }

    @Override
    public void notifyBeforeRemove(Document doc) {
    }

}
