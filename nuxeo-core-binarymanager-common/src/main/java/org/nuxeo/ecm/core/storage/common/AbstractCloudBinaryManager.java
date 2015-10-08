/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.core.storage.common;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.CachingBinaryManager;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public abstract class AbstractCloudBinaryManager extends CachingBinaryManager implements BlobProvider {

    /**
     * Gets the property prefix used in configurarion properties
     */
    protected abstract String getPropertyPrefix();

    protected abstract FileStorage getFileStorage();

    protected abstract BinaryGarbageCollector instantiateGarbageCollector();

    @Override
    public abstract void removeBinaries(Set<String> digests);

    /**
     * Configure Cloud client using properties
     */
    protected abstract void setupCloudClient() throws IOException;

    protected Map<String, String> properties;

    public static final String CACHE_PROPERTY = "cache";

    public static final String DEFAULT_CACHE_SIZE = "100 mb";

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        this.properties = properties;

        setupCloudClient();

        String cacheSizeStr = getProperty(CACHE_PROPERTY, DEFAULT_CACHE_SIZE);
        initializeCache(cacheSizeStr, getFileStorage());
        garbageCollector = instantiateGarbageCollector();
    }

    @Override
    public Blob readBlob(BlobManager.BlobInfo blobInfo) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).readBlob(blobInfo);
    }

    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).writeBlob(blob, doc);
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    protected String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    protected String getProperty(String propertyName, String defaultValue) {
        if (properties.containsKey(propertyName)) {
            return properties.get(propertyName);
        }

        return Framework.getProperty(getConfigurationKey(propertyName), defaultValue);
    }

    public String getConfigurationKey(String propertyName) {
        return String.format("%s.%s", getPropertyPrefix(), propertyName);
    }
}
