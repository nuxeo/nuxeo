/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.elasticsearch.http.readonly.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.elasticsearch.http.readonly.filter.SearchRequestFilter;

/**
 * @since 7.4
 */
@XObject("requestFilter")
public class RequestFilterDescriptor {

    private static final Log log = LogFactory.getLog(RequestFilterDescriptor.class);

    @XNode("@index")
    String index;

    @XNode("@filterClass")
    private Class<? extends SearchRequestFilter> filterClass;

    public RequestFilterDescriptor() {
    }

    public RequestFilterDescriptor(String index, Class<? extends SearchRequestFilter> filterClass) {
        super();
        this.index = index;
        this.filterClass = filterClass;
    }

    public Class<? extends SearchRequestFilter> getFilterClass() {
        return filterClass;
    }

    public String getIndex() {
        return index;
    }

    public void setFilterClass(Class<? extends SearchRequestFilter> filterClass) {
        this.filterClass = filterClass;
    }

    public void setIndex(String index) {
        this.index = index;
    }

}
