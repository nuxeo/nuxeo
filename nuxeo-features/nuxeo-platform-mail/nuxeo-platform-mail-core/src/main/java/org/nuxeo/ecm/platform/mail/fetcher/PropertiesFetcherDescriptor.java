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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.fetcher;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Alexandre Russel
 */
@XObject(value = "propertiesFetcher")
public class PropertiesFetcherDescriptor {

    @XNode("@class")
    private Class<? extends PropertiesFetcher> fetcher;

    @XNode("@name")
    private String name;

    public Class<? extends PropertiesFetcher> getFetcher() {
        return fetcher;
    }

    public void setFetcher(Class<? extends PropertiesFetcher> fetcher) {
        this.fetcher = fetcher;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
