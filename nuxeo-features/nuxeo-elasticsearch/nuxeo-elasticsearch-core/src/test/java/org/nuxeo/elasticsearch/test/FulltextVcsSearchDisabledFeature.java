/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

public class FulltextVcsSearchDisabledFeature extends SimpleFeature {
    private static final String KEY = "nuxeo.test.vcs.fulltext.search.disabled";
    private String flag;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        flag = System.setProperty(KEY, "true");
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        super.stop(runner);
        if (flag == null) {
            System.clearProperty(KEY);
        } else {
            System.setProperty(KEY, flag);
        }
    }

}
