/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.scim.server.tests.compliance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

public class OrderedFeaturesRunner extends FeaturesRunner {

    public OrderedFeaturesRunner(Class<?> arg0) throws InitializationError {
        super(arg0);
    }

    @Override
    protected List computeTestMethods() {
        List list = super.computeTestMethods();
        List copy = new ArrayList(list);
        Collections.sort(copy, new Comparator<FrameworkMethod>() {
            @Override
            public int compare(FrameworkMethod o1, FrameworkMethod o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return copy;
    }
}
