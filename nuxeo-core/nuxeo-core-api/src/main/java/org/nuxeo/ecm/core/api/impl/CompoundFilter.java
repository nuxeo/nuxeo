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
 * A filter based on a list of others filters. To accept a document, all the
 * registered filters must accept it.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
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
            this.filters = new ArrayList<Filter>();
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
