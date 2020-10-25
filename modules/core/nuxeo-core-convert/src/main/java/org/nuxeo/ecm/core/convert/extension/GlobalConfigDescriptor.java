/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.convert.extension;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * XMap Descriptor for the {@link org.nuxeo.ecm.core.convert.api.ConversionService} configuration.
 */
@XObject("configuration")
public class GlobalConfigDescriptor {

    public static final boolean DEFAULT_CACHE_ENABLED = true;

    public static final long DEFAULT_GC_INTERVAL_IN_MIN = 10;

    public static final int DEFAULT_DISK_CACHE_IN_KB = 10 * 1024;

    public static final String DEFAULT_CACHING_DIRECTORY = "convertcache";

    @XNode("enableCache")
    protected Boolean enableCache;

    public boolean isCacheEnabled() {
        return enableCache == null ? DEFAULT_CACHE_ENABLED : enableCache.booleanValue();
    }

    @XNode("cachingDirectory")
    protected String cachingDirectory;

    public String getCachingDirectory() {
        return cachingDirectory == null ? getDefaultCachingDirectory() : cachingDirectory;
    }

    protected String getDefaultCachingDirectory() {
        File cache = new File(Environment.getDefault().getData(), DEFAULT_CACHING_DIRECTORY);
        return cache.getAbsolutePath();
    }

    /** @since 9.1 */
    public void clearCachingDirectory() {
        File cache = new File(getCachingDirectory());
        if (cache.exists()) {
            try {
                FileUtils.deleteDirectory(cache);
            } catch (IOException e) {
                throw new NuxeoException("Cannot create cache dir " + cache, e);
            }
        }
        cache.mkdirs();
    }

    protected Long GCInterval;

    @XNode("gcInterval")
    public void setGCInterval(long value) {
        GCInterval = value == 0 ? null : Long.valueOf(value);
    }

    public long getGCInterval() {
        return GCInterval == null ? DEFAULT_GC_INTERVAL_IN_MIN : GCInterval.longValue();
    }

    protected Integer diskCacheSize;

    @XNode("diskCacheSize")
    public void setDiskCacheSize(int size) {
        diskCacheSize = size == 0 ? null : Integer.valueOf(size);
    }

    public int getDiskCacheSize() {
        return diskCacheSize == null ? DEFAULT_DISK_CACHE_IN_KB : diskCacheSize.intValue();
    }

    public void update(GlobalConfigDescriptor other) {
        if (other.enableCache != null) {
            enableCache = other.enableCache;
        }
        if (other.GCInterval != null) {
            GCInterval = other.GCInterval;
        }
        if (other.diskCacheSize != null) {
            diskCacheSize = other.diskCacheSize;
        }
        if (other.cachingDirectory != null) {
            cachingDirectory = other.cachingDirectory;
        }
    }

}
