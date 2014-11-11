/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features({ TransactionalFeature.class, PlatformFeature.class })
@Deploy({ "org.nuxeo.runtime.datasource", "org.nuxeo.ecm.core.persistence", "org.nuxeo.ecm.platform.audit"})
@LocalDeploy("org.nuxeo.ecm.platform.audit:nxaudit-ds.xml")
public class AuditFeature extends SimpleFeature {

    protected static final String DIRECTORY = "target/test/nxaudit";
    protected static final String PROP_NAME = "ds.nxaudit.home";

    protected File dir;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        dir = new File(DIRECTORY);
        FileUtils.deleteTree(dir);
        dir.mkdirs();
        System.setProperty(PROP_NAME, dir.getPath());
        super.initialize(runner);
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        FileUtils.deleteTree(dir);
        dir = null;
        super.stop(runner);
    }
}
