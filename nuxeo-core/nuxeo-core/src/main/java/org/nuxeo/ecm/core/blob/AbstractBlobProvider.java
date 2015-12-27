/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract implementation for {@link BlobProvider} providing common logic.
 *
 * @since 7.10
 */
public abstract class AbstractBlobProvider implements BlobProvider {

    public String blobProviderId;

    public Map<String, String> properties;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        this.blobProviderId = blobProviderId;
        this.properties = properties;
    }

    protected boolean supportsUserUpdateDefaultTrue() {
        return !Boolean.parseBoolean(properties.get(PREVENT_USER_UPDATE));
    }

    protected boolean supportsUserUpdateDefaultFalse() {
        return !Boolean.parseBoolean(properties.getOrDefault(PREVENT_USER_UPDATE, "true"));
    }

    @Override
    public boolean supportsUserUpdate() {
        return supportsUserUpdateDefaultTrue();
    }

}
