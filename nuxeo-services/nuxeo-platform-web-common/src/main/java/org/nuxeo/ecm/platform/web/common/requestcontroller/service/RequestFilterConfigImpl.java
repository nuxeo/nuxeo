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

package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

/**
 * Basic implementation of the {@link RequestFilterConfig} interface.
 *
 * @author tiry
 */
public class RequestFilterConfigImpl implements RequestFilterConfig {

    private static final long serialVersionUID = 1L;

    protected final boolean useTx;

    protected final boolean useSync;

    protected final boolean isPrivate;

    protected final boolean cached;

    protected final String cacheTime;

    public RequestFilterConfigImpl(boolean useSync, boolean useTx, boolean cached, boolean isPrivate, String cacheTime) {
        this.useSync = useSync;
        this.useTx = useTx;
        this.cached = cached;
        this.isPrivate = isPrivate;
        this.cacheTime = cacheTime;
    }

    public boolean needSynchronization() {
        return useSync;
    }

    public boolean needTransaction() {
        return useTx;
    }

    public boolean isCached() {
        return cached;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getCacheTime() {
        return cacheTime;
    }

}
