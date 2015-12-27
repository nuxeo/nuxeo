/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.extension;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap Descriptor for the {@link org.nuxeo.ecm.core.convert.api.ConversionService} configuration.
 *
 * @author tiry
 */
@XObject("configuration")
public class GlobalConfigDescriptor implements Serializable {

    public static final long DEFAULT_GC_INTERVAL_IN_MIN = 10;

    public static final int DEFAULT_DISK_CACHE_IN_KB = 10 * 1024;

    private static final String CACHING_DIRECTORY = "convertcache";

    private static final long serialVersionUID = 1L;

    protected long GCInterval = DEFAULT_GC_INTERVAL_IN_MIN;

    protected int diskCacheSize = DEFAULT_DISK_CACHE_IN_KB;

    @XNode("enableCache")
    protected boolean enableCache = true;

    @XNode("cachingDirectory")
    protected String cachingDirectory = defaultCachingDirectory().getAbsolutePath();

    public long getGCInterval() {
        return GCInterval;
    }

    @XNode("gcInterval")
    public void setGCInterval(long value) {
        GCInterval = value == 0 ? DEFAULT_GC_INTERVAL_IN_MIN : value;
    }

    public int getDiskCacheSize() {
        return diskCacheSize;
    }

    @XNode("diskCacheSize")
    public void setDiskCacheSize(int size) {
        diskCacheSize = size == 0 ? DEFAULT_DISK_CACHE_IN_KB : size;
    }

    public boolean isCacheEnabled() {
        return enableCache;
    }

    public void update(GlobalConfigDescriptor other) {
        if (other.GCInterval != DEFAULT_GC_INTERVAL_IN_MIN) {
            GCInterval = other.GCInterval;
        }
        if (other.diskCacheSize != DEFAULT_GC_INTERVAL_IN_MIN) {
            diskCacheSize = other.diskCacheSize;
        }

        if (other.cachingDirectory != defaultCachingDirectory().getAbsolutePath()) {
            cachingDirectory = other.cachingDirectory;
        }

        enableCache = other.enableCache;
    }

    protected File defaultCachingDirectory() {
        File data = new File(Environment.getDefault().getData(), CACHING_DIRECTORY);
        if (data.exists()) {
            try {
                FileUtils.deleteDirectory(data);
            } catch (IOException cause) {
                throw new RuntimeException("Cannot create cache dir " + data, cause);
            }
        }
        data.mkdirs();
        return data.getAbsoluteFile();
    }

    public String getCachingDirectory() {
        return cachingDirectory;
    }

}
