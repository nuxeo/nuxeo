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

    protected final boolean useTxBuffered;

    protected final boolean cached;

    protected final String cacheTime;

    public RequestFilterConfigImpl(boolean useSync, boolean useTx, boolean useTxBuffered, boolean cached,
            boolean isPrivate, String cacheTime) {
        this.useSync = useSync;
        this.useTx = useTx;
        this.useTxBuffered = useTxBuffered;
        this.cached = cached;
        this.isPrivate = isPrivate;
        this.cacheTime = cacheTime;
    }

    @Override
    public boolean needSynchronization() {
        return useSync;
    }

    @Override
    public boolean needTransaction() {
        return useTx;
    }

    @Override
    public boolean needTransactionBuffered() {
        return useTxBuffered;
    }

    @Override
    public boolean isCached() {
        return cached;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public String getCacheTime() {
        return cacheTime;
    }

}
