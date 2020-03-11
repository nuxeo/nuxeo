/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.blob;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.blob.AbstractBlobStoreConfiguration;
import org.nuxeo.ecm.core.blob.CachingConfiguration;
import org.nuxeo.ecm.core.blob.DigestConfiguration;

/**
 * Abstract blob store configuration for cloud providers.
 *
 * @since 11.1
 */
public abstract class CloudBlobStoreConfiguration extends AbstractBlobStoreConfiguration {

    public static final String DIRECTDOWNLOAD_PROPERTY = "directdownload";

    public static final String DEFAULT_DIRECTDOWNLOAD = "false";

    public static final String DIRECTDOWNLOAD_EXPIRE_PROPERTY = "directdownload.expire";

    public static final long DEFAULT_DIRECTDOWNLOAD_EXPIRE = 60L * 60L; // 1h

    public static final String DIGEST_ALGORITHM_PROPERTY = "digest";

    public final DigestConfiguration digestConfiguration;

    public final CachingConfiguration cachingConfiguration;

    public final boolean directDownload;

    public final long directDownloadExpire;

    public CloudBlobStoreConfiguration(String systemPropertyPrefix, Map<String, String> properties) throws IOException {
        super(systemPropertyPrefix, properties);

        digestConfiguration = new DigestConfiguration(systemPropertyPrefix, properties);
        cachingConfiguration = new CachingConfiguration(systemPropertyPrefix, properties);

        directDownload = parseDirectDownload();
        directDownloadExpire = parseDirectDownloadExpire();
    }

    protected boolean parseDirectDownload() {
        return Boolean.parseBoolean(getProperty(DIRECTDOWNLOAD_PROPERTY, DEFAULT_DIRECTDOWNLOAD));
    }

    protected long parseDirectDownloadExpire() {
        long expire = getLongProperty(DIRECTDOWNLOAD_EXPIRE_PROPERTY);
        if (expire < 0) {
            expire = DEFAULT_DIRECTDOWNLOAD_EXPIRE;
        }
        return expire;
    }

}
