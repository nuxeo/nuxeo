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
