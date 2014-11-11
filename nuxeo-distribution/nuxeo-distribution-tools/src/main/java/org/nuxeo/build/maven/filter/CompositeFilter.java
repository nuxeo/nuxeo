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
 *     bstefanescu
 */
package org.nuxeo.build.maven.filter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.build.maven.ArtifactDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class CompositeFilter implements Filter {

    protected List<Filter> filters = new ArrayList<Filter>();

    public CompositeFilter() {
    }

    public CompositeFilter(List<Filter> filters) {
        this.filters.addAll(filters);
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public void addFilters(List<Filter> filters) {
        this.filters.addAll(filters);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }

    public void addFiltersFromPattern(String pattern) {
        addFiltersFromDescriptor(new ArtifactDescriptor(pattern));
    }

    public void addFiltersFromDescriptor(ArtifactDescriptor ad) {
        if (ad.groupId != null && !ad.groupId.equals("*")) {
            addFilter(new GroupIdFilter(ad.groupId));
        }
        if (ad.artifactId != null && !ad.artifactId.equals("*")) {
            addFilter(new ArtifactIdFilter(ad.artifactId));
        }
        if (ad.version != null && !ad.version.equals("*")) {
            addFilter(new VersionFilter(ad.version));
        }
        if (ad.type != null && !ad.type.equals("*")) {
            addFilter(new TypeFilter(ad.type));
        }
        if (ad.classifier != null && !ad.classifier.equals("*")) {
            addFilter(new ClassifierFilter(ad.classifier));
        }
        if (ad.scope != null && !ad.scope.equals("*")) {
            addFilter(new ScopeFilter(ad.scope));
        }
    }

    public static Filter compact(CompositeFilter filter) {
        Filter result = filter;
        CompositeFilter cf = filter;
        if (cf.filters.size() == 1) {
            result = cf.filters.get(0);
            if (result instanceof CompositeFilter) {
                result = compact((CompositeFilter)result);
            }
        }
        return result;
    }

}
