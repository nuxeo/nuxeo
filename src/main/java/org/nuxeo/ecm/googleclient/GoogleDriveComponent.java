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
package org.nuxeo.ecm.googleclient;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component for the management of Google Drive blobs.
 *
 * @since 7.2
 */
public class GoogleDriveComponent extends DefaultComponent {

    public static final String GOOGLE_DRIVE_PREFIX = "googledrive";

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        BlobManager blobManager = Framework.getService(BlobManager.class);
        blobManager.registerBlobProvider(GOOGLE_DRIVE_PREFIX, new GoogleDriveBlobProvider());
    }

    @Override
    public void deactivate(ComponentContext context) {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        blobManager.unregisterBlobProvider(GOOGLE_DRIVE_PREFIX);
        super.deactivate(context);
    }

}
