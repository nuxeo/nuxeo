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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;

public class EventDescriptorPage implements Serializable {

    private static final long serialVersionUID = 876567561L;

    private final int pageIndex;

    private final boolean bHasMorePage;

    private EventDescriptor[] events;

    public EventDescriptorPage() {
        super();
        pageIndex = 0;
        bHasMorePage = false;
    }

    public EventDescriptorPage(EventDescriptor[] data, int pageIndex,
            boolean bHasMorePage) {

        this.pageIndex = pageIndex;
        this.bHasMorePage = bHasMorePage;
        this.events = data;

    }

    public int getPageIndex() {
        return pageIndex;
    }

    public boolean hasModePage() {
        return bHasMorePage;
    }

    public EventDescriptor[] getEvents() {
        return events;
    }

    public boolean getHasMorePage() {
        return bHasMorePage;
    }

}
