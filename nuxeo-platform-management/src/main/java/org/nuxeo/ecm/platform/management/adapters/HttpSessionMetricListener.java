/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.adapters;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class HttpSessionMetricListener implements HttpSessionBindingListener {

    protected HttpSessionMetricListener(HttpSessionMetricAdapter adapter) {
        this.adapter = adapter;
    }
    
    protected final HttpSessionMetricAdapter adapter;
    
    public void valueBound(HttpSessionBindingEvent event) {
        adapter.createdSessionCount += 1;
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
        adapter.destroyedSessionCount += 1;
    }

}