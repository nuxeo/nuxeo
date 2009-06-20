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
package org.nuxeo.build.ant.artifact;

import org.apache.tools.ant.types.DataType;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.NotFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FiltersCollection extends DataType {

    public AndFilter filter;

    public void addExcludes(Excludes excludes) {
        if (filter == null) {
            filter = new AndFilter();
        }
        filter.addFilter(new NotFilter(excludes.filter));
    }

    public void addIncludes(Includes includes) {
        if (filter == null) {
            filter = new AndFilter();
        }
        filter.addFilter(includes.filter);
    }

}
