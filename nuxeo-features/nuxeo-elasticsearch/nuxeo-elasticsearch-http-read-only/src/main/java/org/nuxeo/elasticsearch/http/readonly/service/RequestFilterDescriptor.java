/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.elasticsearch.http.readonly.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.4
 */
@XObject("requestFilter")
public class RequestFilterDescriptor {

    private static final Log log = LogFactory.getLog(RequestFilterDescriptor.class);

    @XNode("@index")
    String index;

    @XNode("@filterClass")
    private Class filterClass;

    public RequestFilterDescriptor() {
    }

    public RequestFilterDescriptor(String index, Class filterClass) {
        super();
        this.index = index;
        this.filterClass = filterClass;
    }

    public Class getFilterClass() {
        return filterClass;
    }

    public String getIndex() {
        return index;
    }

    public void setFilterClass(Class filterClass) {
        this.filterClass = filterClass;
    }

    public void setIndex(String index) {
        this.index = index;
    }

}
