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

import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;

/**
 * XMap Descriptor for the {@link org.nuxeo.ecm.core.convert.api.ConversionService} configuration.
 */
@XObject("configuration")
@XRegistry
public class GlobalConfigDescriptor {

    public static final long DEFAULT_GC_INTERVAL_IN_MIN = 10;

    public static final int DEFAULT_DISK_CACHE_IN_KB = 10 * 1024;

    public static final String DEFAULT_CACHING_DIRECTORY = "convertcache";

    @XNode(value = "enableCache")
    protected boolean enableCache = true;

    @XNode("cachingDirectory")
    protected String cachingDirectory;

    @XNode("gcInterval")
    protected Long GCInterval;

    @XNode("diskCacheSize")
    protected Integer diskCacheSize;

    public boolean isCacheEnabled() {
        return enableCache;
    }

    public String getCachingDirectory() {
        return cachingDirectory == null ? getDefaultCachingDirectory() : cachingDirectory;
    }

    protected String getDefaultCachingDirectory() {
        File cache = new File(Environment.getDefault().getData(), DEFAULT_CACHING_DIRECTORY);
        return cache.getAbsolutePath();
    }

    public long getGCInterval() {
        return GCInterval == null ? DEFAULT_GC_INTERVAL_IN_MIN : GCInterval.longValue();
    }

    public int getDiskCacheSize() {
        return diskCacheSize == null ? DEFAULT_DISK_CACHE_IN_KB : diskCacheSize.intValue();
    }

}
