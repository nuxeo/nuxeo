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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

public class FakeBlobProviderComponent extends DefaultComponent {

    protected static final String FAKE_BLOB_PROVIDER_PREFIX = "fake";

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = new FakeBlobProviderImpl();
        blobManager.registerBlobProvider(FAKE_BLOB_PROVIDER_PREFIX, blobProvider);
    }

    @Override
    public void deactivate(ComponentContext context) {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        blobManager.unregisterBlobProvider(FAKE_BLOB_PROVIDER_PREFIX);
        super.deactivate(context);
    }

}
