/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.jboss.deployment;

import java.net.URL;

import junit.framework.TestCase;

// TODO: NuxeoDeploymentSorter not used. Remove test ?
public class TestNuxeoDeploymentSorter extends TestCase {

    public void test() throws Exception {
        // NuxeoDeploymentSorter sorter = new NuxeoDeploymentSorter();

        URL url1 = new URL("http://www.nuxeo.com/");
        URL url2 = new URL("http://www.nuxeo.com/");
        URL url3 = new URL("http://www.nuxeo.com/");
        URL url4 = new URL("http://www.nuxeo.com/");

        ////assertEquals(0, sorter.compare(url1, url1));
    }

}
