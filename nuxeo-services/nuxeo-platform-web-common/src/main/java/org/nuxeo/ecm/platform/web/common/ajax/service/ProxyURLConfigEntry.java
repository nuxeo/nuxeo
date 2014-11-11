/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.ajax.service;

/**
 *
 *
 * @author tiry
 *
 */
public class ProxyURLConfigEntry {

    protected boolean granted = false;

    protected String descriptorName;

    protected boolean useCache;

    protected boolean cachePerSession;


    public ProxyURLConfigEntry() {
        this.granted=false;
    }

    public ProxyURLConfigEntry( boolean granted, ProxyableURLDescriptor desc) {
        this.granted=granted;
        this.descriptorName=desc.getName();
        useCache = desc.useCache;
        cachePerSession = desc.cachePerSession;
    }

    public boolean isGranted() {
        return granted;
    }

    public String getDescriptorName() {
        return descriptorName;
    }

    public boolean useCache() {
        return useCache;
    }

    public boolean isCachePerSession() {
        if (!useCache) {
            return false;
        }
        return cachePerSession;
    }




}
