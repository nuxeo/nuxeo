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
 *     Nelson Silva
 */
package org.nuxeo.ecm.googleclient;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.googleclient.credential.CredentialFactory;
import org.nuxeo.ecm.googleclient.credential.ServiceAccountCredentialFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component for the management of Google Drive blobs.
 *
 * @since 7.3
 */
public class GoogleDriveComponent extends DefaultComponent {

    public static final String GOOGLE_DRIVE_PREFIX = "googledrive";

    // Service account details
    public static final String SERVICE_ACCOUNT_ID_PROP = "nuxeo.google.serviceAccountId";

    public static final String SERVICE_ACCOUNT_P12_PATH_PROP = "nuxeo.google.serviceAccountP12Path";

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        // Service account configuration
        String serviceAccountId = Framework.getProperty(SERVICE_ACCOUNT_ID_PROP);
        if (StringUtils.isBlank(serviceAccountId)) {
            throw new NuxeoException("Missing value for property: " + SERVICE_ACCOUNT_ID_PROP);
        }
        String p12 = Framework.getProperty(SERVICE_ACCOUNT_P12_PATH_PROP);
        if (StringUtils.isBlank(p12)) {
            throw new NuxeoException("Missing value for property: " + SERVICE_ACCOUNT_P12_PATH_PROP);
        }
        java.io.File p12File = new java.io.File(p12);
        if (!p12File.exists()) {
            throw new NuxeoException("No such file: " + p12 + " for property: " + SERVICE_ACCOUNT_P12_PATH_PROP);
        }
        CredentialFactory credentialFactory = new ServiceAccountCredentialFactory(serviceAccountId, p12File);

        BlobManager blobManager = Framework.getService(BlobManager.class);
        GoogleDriveBlobProvider blobProvider = new GoogleDriveBlobProvider(credentialFactory);
        blobManager.registerBlobProvider(GOOGLE_DRIVE_PREFIX, blobProvider);
    }

    @Override
    public void deactivate(ComponentContext context) {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        blobManager.unregisterBlobProvider(GOOGLE_DRIVE_PREFIX);
        super.deactivate(context);
    }

}
