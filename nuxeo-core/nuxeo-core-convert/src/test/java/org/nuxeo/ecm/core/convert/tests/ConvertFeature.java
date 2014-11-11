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
 *     ataillefer
 */
package org.nuxeo.ecm.core.convert.tests;

import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @since 6.0
 */
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.core.api", "org.nuxeo.ecm.core.convert.api",
        "org.nuxeo.ecm.core.convert" })
public class ConvertFeature extends SimpleFeature {

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // do testing configuration
        // cachesize = -1 (actually 0)
        // GC interval negative => interpreted as seconds
        ConversionServiceImpl.setMaxCacheSizeInKB(-1);
        ConversionServiceImpl.setGCIntervalInMinutes(-1000);
    }
}
