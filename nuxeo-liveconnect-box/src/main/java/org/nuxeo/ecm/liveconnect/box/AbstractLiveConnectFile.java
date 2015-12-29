/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.box;

import java.util.Objects;

import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Basic implementation of {@link LiveConnectFile}.
 */
public abstract class AbstractLiveConnectFile implements LiveConnectFile {

    private static final long serialVersionUID = 1L;

    private LiveConnectFileInfo info;

    private String mimeType;

    public AbstractLiveConnectFile(LiveConnectFileInfo info) {
        this.info = Objects.requireNonNull(info);
    }

    @Override
    public final LiveConnectFileInfo getInfo() {
        return info;
    }

    /**
     * Should be overriden by subclasses wanting to rely on a different field as mime type.
     */
    @Override
    public String getMimeType() {
        if (mimeType == null) {
            MimetypeRegistryService service = (MimetypeRegistryService) Framework.getLocalService(MimetypeRegistry.class);
            mimeType = service.getMimetypeFromFilename(getFilename());
        }
        return mimeType;
    }

    /**
     * Should be overriden by subclasses wanting to rely on a different field as encoding.
     */
    @Override
    public String getEncoding() {
        // TODO extract from mimeType
        return null;
    }
}
