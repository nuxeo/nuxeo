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

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.convert.api.ConversionService;

/**
 * XMap Descriptor for the {@link ConversionService} configuration
 * @author tiry
 *
 */
@XObject("configuration")
public class GlobalConfigDescriptor implements Serializable{

    /**
     *
     */

    private static final long serialVersionUID = 1L;

    public static final long DEFAULT_GC_INTERVAL_IN_MIN = 10;

    public static final int DEFAULT_DISK_CACHE_IN_KB= 10 * 1024;


    @XNode("gcInterval")
    protected long GCInterval;

    @XNode("diskCacheSize")
    protected int diskCacheSize;

    @XNode("enableCache")
    protected boolean enableCache=true;


    @XNode("cachingDirectory")
    protected String cachingDirectory;


    public long getGCInterval() {
        if (GCInterval==0) {
            return DEFAULT_GC_INTERVAL_IN_MIN;
        }
        return GCInterval;
    }

    public int getDiskCacheSize() {
        if (diskCacheSize==0) {
            return DEFAULT_DISK_CACHE_IN_KB;
        }
        return diskCacheSize;
    }


    public boolean isCacheEnabled() {
        return enableCache;
    }

    public void update(GlobalConfigDescriptor other) {

        if (other.GCInterval!=0) {
            GCInterval= other.GCInterval;
        }
        if (other.diskCacheSize!=0) {
            diskCacheSize = other.diskCacheSize;
        }

        if (other.cachingDirectory!=null) {
            cachingDirectory = other.cachingDirectory;
        }

        enableCache = other.enableCache;
    }


    public String getCachingDirectory() {
        if (cachingDirectory==null) {
            cachingDirectory=System.getProperty("java.io.tmpdir");
        }
        return cachingDirectory;
    }
}
