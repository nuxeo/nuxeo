/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.ajax.service;

/**
 * @author tiry
 */
public class ProxyURLConfigEntry {

    protected boolean granted = false;

    protected String descriptorName;

    protected boolean useCache;

    protected boolean cachePerSession;

    public ProxyURLConfigEntry() {
        granted = false;
    }

    public ProxyURLConfigEntry(boolean granted, ProxyableURLDescriptor desc) {
        this.granted = granted;
        descriptorName = desc.getName();
        useCache = desc.useCache;
        cachePerSession = desc.cachePerSession;
    }

    public boolean isGranted() {
        return granted;
    }

    public boolean useCache() {
        return useCache;
    }

}
