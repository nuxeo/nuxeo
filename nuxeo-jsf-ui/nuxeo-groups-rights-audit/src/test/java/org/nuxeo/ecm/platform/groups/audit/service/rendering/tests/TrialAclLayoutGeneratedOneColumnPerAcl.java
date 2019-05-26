/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilderMultiColumn;
import org.nuxeo.ecm.platform.groups.audit.service.acl.IAclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsAllContent;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test excel export of groups
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
public class TrialAclLayoutGeneratedOneColumnPerAcl extends AbstractAclLayoutTest {
    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    private final static Log log = LogFactory.getLog(TrialAclLayoutGeneratedOneColumnPerAcl.class);

    protected static File testFile = new File(folder + TrialAclLayoutGeneratedOneColumnPerAcl.class.getSimpleName()
            + ".xls");

    @Test
    public void testExcelExportReport() throws Exception {
        // doc tree
        int depth = 3;
        int width = 10;
        int groups = 30;

        log.info("Build a test repository: depth=" + depth + ", width:" + width + ", groups:" + groups);
        makeDocumentTree(session, depth, width, groups);
        session.save();
        log.info("done building test data");

        // --------------------
        // settings and filters
        ReportLayoutSettings s = AclExcelLayoutBuilderMultiColumn.defaultLayout();
        IContentFilter filter = new AcceptsAllContent();

        // generate XLS report
        log.info("Start audit");
        IAclExcelLayoutBuilder v = new AclExcelLayoutBuilderMultiColumn(s, filter);
        v.renderAudit(session);

        // save and finish
        log.info("End audit");
        v.getExcel().save(testFile);
        log.info("End save");
    }
}
