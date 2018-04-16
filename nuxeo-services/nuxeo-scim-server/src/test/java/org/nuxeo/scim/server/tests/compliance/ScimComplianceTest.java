/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.tests.compliance;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import info.simplecloud.core.Group;
import info.simplecloud.core.User;
import info.simplecloud.scimproxy.compliance.CSP;
import info.simplecloud.scimproxy.compliance.enteties.ReadableTestResult;
import info.simplecloud.scimproxy.compliance.enteties.TestResult;
import info.simplecloud.scimproxy.compliance.test.ConfigTest;
import info.simplecloud.scimproxy.compliance.test.DeleteTest;
import info.simplecloud.scimproxy.compliance.test.PutTest;
import info.simplecloud.scimproxy.compliance.test.ResourceCache;
import info.simplecloud.scimproxy.compliance.test.WorkingPostTest;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.scim.server.tests.ScimFeature;
import org.nuxeo.scim.server.tests.ScimServerInit;

import com.google.inject.Inject;

@RunWith(OrderedFeaturesRunner.class)
@Features({ ScimFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.CLASS, init = ScimServerInit.class)
public class ScimComplianceTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    CoreSession session;

    protected static CSP csp = null;

    protected static int testIdx = 0;

    protected static ArrayList<TestResult> results;

    protected static ResourceCache<User> userCache;

    protected static ResourceCache<Group> groupCache;

    protected static ConfigTest configTest;

    @Before
    public void checkNotOracle() {
        // TODO debug test3Update and test4Delete which fail on Oracle (null vs "" somewhere)
        assumeTrue(!coreFeature.getStorageConfiguration().isVCSOracle());
    }

    @BeforeClass
    public static void initComplianceSuite() {
        csp = new CSP();
        csp.setUrl("http://localhost:18090/scim");
        csp.setVersion("/v1");
        csp.setAuthentication("basicAuth");
        csp.setUsername("Administrator");
        csp.setPassword("Administrator");
        results = new ArrayList<TestResult>();
        userCache = new ResourceCache<User>();
        groupCache = new ResourceCache<Group>();
        configTest = new ConfigTest();
    }

    protected void verifyTests() {
        for (int i = testIdx; i < results.size(); i++) {
            ReadableTestResult result = new ReadableTestResult(results.get(i));
            System.out.println(result.getDisplay());
            assertTrue(result.getErrorMessage(), !result.isFailed());
        }
        testIdx = results.size();
    }

    @Test
    public void test1Config() throws Exception {
        results.add(configTest.getConfiguration(csp));
        // Schemas
        results.add(configTest.getSchema("Users", csp));
        results.add(configTest.getSchema("Groups", csp));

        verifyTests();
    }

    @Test
    public void test2Create() throws Exception {
        results.addAll(new WorkingPostTest(csp, userCache, groupCache).run());
        verifyTests();
    }

    @Test
    public void test3Update() throws Exception {
        results.addAll(new PutTest(csp, userCache, groupCache).run());
        verifyTests();
    }

    @Test
    public void test4Delete() throws Exception {
        results.addAll(new DeleteTest(csp, userCache, groupCache).run());
        verifyTests();
    }

    @AfterClass
    public static void finish() {

        int nbRun = results.size();
        int nbSuccess = 0;
        int nbSkipped = 0;
        int nbFailed = 0;
        for (TestResult res : results) {
            if (res.getStatus() == TestResult.SUCCESS) {
                nbSuccess++;
            } else if (res.getStatus() == TestResult.SKIPPED) {
                nbSkipped++;
            } else if (res.getStatus() == TestResult.ERROR) {
                nbFailed++;
            }
        }

        System.out.println("Ran " + nbRun + " compliancy tests : ");
        System.out.println("   " + nbSuccess + " success ");
        System.out.println("   " + nbSkipped + " skipped ");
        System.out.println("   " + nbFailed + " failed ");

    }

}
