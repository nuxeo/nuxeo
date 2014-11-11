/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.runtime.test.runner;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.distrib.DistributionFeature;
import org.nuxeo.runtime.test.runner.distrib.NuxeoDistribution;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Ignore("distribution dependency is causing class loader pb.")
@RunWith(FeaturesRunner.class)
@Features(DistributionFeature.class)
@NuxeoDistribution(profile="core-5.3.1-SNAPSHOT")
public class DistributionTest {

    @Test public void testAgainstDistribution() throws Exception {
        System.out.println(Framework.getProperties());
    }

}
