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

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class HttpSessionMetricAdapter implements HttpSessionMetricMBean {

    protected long createdSessionCount = 0L;

    protected long destroyedSessionCount = 0L;

    public long getActiveSessionCount() {
        return createdSessionCount - destroyedSessionCount;
    }

    public long getDestroyedSessionCount() {
        return destroyedSessionCount;
    }

    public long getCreatedSessionCount() {
        return createdSessionCount;
    }

}
