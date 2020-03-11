/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on a list of others filters. To accept a document, all the registered filters must accept it.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class CompoundFilter implements Filter {

    private static final long serialVersionUID = 5755619357049775017L;

    private final List<Filter> filters;

    /**
     * Generic constructor.
     *
     * @param filters
     */
    public CompoundFilter(Filter... filters) {
        this(Arrays.asList(filters));
    }

    /**
     * Generic constructor.
     *
     * @param filters
     */
    public CompoundFilter(List<Filter> filters) {
        if (filters == null) {
            this.filters = new ArrayList<>();
        } else {
            this.filters = filters;
        }
    }

    @Override
    public boolean accept(DocumentModel docModel) {
        for (Filter filter : filters) {
            if (!filter.accept(docModel)) {
                return false;
            }
        }
        return true;
    }

}
