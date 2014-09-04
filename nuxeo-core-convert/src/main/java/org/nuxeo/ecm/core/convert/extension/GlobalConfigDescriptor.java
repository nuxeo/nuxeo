/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap Descriptor for the
 * {@link org.nuxeo.ecm.core.convert.api.ConversionService} configuration.
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
    protected String cachingDirectory = defaultCachingDirectory().getAbsolutePath();

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

    public void setDiskCacheSize(int size) {
        diskCacheSize = size;
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
