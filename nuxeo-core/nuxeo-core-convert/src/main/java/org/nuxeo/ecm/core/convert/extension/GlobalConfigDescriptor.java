/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.extension;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap Descriptor for the {@link ConversionService} configuration.
 *
 * @author tiry
 */
@XObject("configuration")
public class GlobalConfigDescriptor implements Serializable {

    public static final long DEFAULT_GC_INTERVAL_IN_MIN = 10;

    public static final int DEFAULT_DISK_CACHE_IN_KB = 10 * 1024;

    private static final String CACHING_DIRECTORY = "convertcache";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(GlobalConfigDescriptor.class);

    @XNode("gcInterval")
    protected long GCInterval;

    @XNode("diskCacheSize")
    protected int diskCacheSize;

    @XNode("enableCache")
    protected boolean enableCache = true;

    @XNode("cachingDirectory")
    protected String cachingDirectory;

    public long getGCInterval() {
        if (GCInterval == 0) {
            return DEFAULT_GC_INTERVAL_IN_MIN;
        }
        return GCInterval;
    }

    public int getDiskCacheSize() {
        if (diskCacheSize == 0) {
            return DEFAULT_DISK_CACHE_IN_KB;
        }
        return diskCacheSize;
    }

    public boolean isCacheEnabled() {
        return enableCache;
    }

    public void update(GlobalConfigDescriptor other) {
        if (other.GCInterval != 0) {
            GCInterval = other.GCInterval;
        }
        if (other.diskCacheSize != 0) {
            diskCacheSize = other.diskCacheSize;
        }

        if (other.cachingDirectory != null) {
            cachingDirectory = other.cachingDirectory;
        }

        enableCache = other.enableCache;
    }

    public String getCachingDirectory() {
        if (cachingDirectory == null) {
            File cacheFile = new File(System.getProperty("java.io.tmpdir"), CACHING_DIRECTORY);
            if (cacheFile.exists() && !cacheFile.canWrite()) {
                log.debug("change directory to avoid FileNotFoundException (permission denied)");
                try {
                    cacheFile = File.createTempFile(CACHING_DIRECTORY, null, cacheFile.getParentFile());
                    cacheFile.delete();
                } catch (IOException e) {
                    log.error("Could not create caching directory", e);
                }
            }
            cacheFile.mkdirs();
            cachingDirectory = cacheFile.getPath();
        }
        return cachingDirectory;
    }
}
