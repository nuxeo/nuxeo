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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.CREATE_FROM_KEY_GROUPS;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.CREATE_FROM_KEY_USERS;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.PREVENT_USER_UPDATE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.TRANSIENT;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;

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

    @Override
    public boolean isTransient() {
        return Boolean.parseBoolean(properties.get(TRANSIENT));
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean hasCreateFromKeyPermission() {
        NuxeoPrincipal principal = ClientLoginModule.getCurrentPrincipal();
        if (principal == null) {
            return false;
        }

        String createFromKeyUsers = properties.getOrDefault(CREATE_FROM_KEY_USERS, EMPTY);
        String createFromKeyGroups = properties.getOrDefault(CREATE_FROM_KEY_GROUPS, EMPTY);

        if ("*".equals(createFromKeyUsers) || "*".equals(createFromKeyGroups)) {
            return true;
        }
        List<String> authorizedUsers = Arrays.asList(createFromKeyUsers.split(","));
        List<String> authorizedGroups = Arrays.asList(createFromKeyGroups.split(","));

        return principal.isAdministrator() || authorizedUsers.contains(principal.getName())
                || authorizedGroups.stream().anyMatch(principal::isMemberOf);
    }

}
