/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.core;

import java.util.Objects;

import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.api.Framework;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Basic implementation of {@link LiveConnectFile}.
 *
 * @since 8.1
 */
public abstract class AbstractLiveConnectFile implements LiveConnectFile {

    private static final long serialVersionUID = 1L;

    private final LiveConnectFileInfo info;

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
            MimetypeRegistryService service = (MimetypeRegistryService) Framework.getService(MimetypeRegistry.class);
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

    @Override
    public String toString() {
        ToStringHelper helper = MoreObjects.toStringHelper(this);
        helper.add("mimeType", getMimeType());
        helper.add("encoding", getEncoding());
        helper.add("filename", getFilename());
        helper.add("fileSize", getFileSize());
        helper.add("digest", getDigest());
        helper.add("info", getInfo());
        return helper.toString();
    }
}
