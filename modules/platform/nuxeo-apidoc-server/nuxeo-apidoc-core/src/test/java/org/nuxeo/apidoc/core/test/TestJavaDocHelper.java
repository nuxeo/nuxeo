/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.apidoc.documentation.JavaDocHelper;
import org.nuxeo.ecm.automation.core.impl.OperationChainCompiler;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestJavaDocHelper {

    final String DISTRIB_NAME = "foo"; // distrib name does not have any effect anymore

    protected void check(String expected, String actual) throws IOException {
        assertEquals(expected, actual);
        checkReachable(expected, false);
    }

    /**
     * Helps checking for the actual response code.
     */
    protected void checkReachable(String expected, boolean doCheck) throws IOException {
        assumeTrue(doCheck);
        HttpURLConnection huc = (HttpURLConnection) new URL(expected).openConnection();
        huc.setRequestMethod("HEAD");
        assertEquals(HttpURLConnection.HTTP_OK, huc.getResponseCode());
    }

    @Test
    public void testGetUrl() throws IOException {
        JavaDocHelper helper = JavaDocHelper.getHelper(DISTRIB_NAME, "10.10");
        // service
        check("https://community.nuxeo.com/api/nuxeo/release-10.10/javadoc/org/nuxeo/ecm/core/api/repository/RepositoryManager.html",
                helper.getUrl("org.nuxeo.ecm.core.api.repository.RepositoryManager"));
        // operation
        check("https://community.nuxeo.com/api/nuxeo/release-10.10/javadoc/org/nuxeo/ecm/automation/core/operations/document/UpdateDocument.html",
                helper.getUrl("org.nuxeo.ecm.automation.core.operations.document.UpdateDocument"));
        // check chain special use case
        check("https://community.nuxeo.com/api/nuxeo/release-10.10/javadoc/org/nuxeo/ecm/automation/core/impl/OperationChainCompiler.CompiledChainImpl.html",
                helper.getUrl(OperationChainCompiler.class.getCanonicalName(), "CompiledChainImpl"));
        // Seam component
        check("https://community.nuxeo.com/api/nuxeo/release-10.10/javadoc/org/nuxeo/ecm/webapp/action/ActionContextProvider.html",
                helper.getUrl("org.nuxeo.ecm.webapp.action.ActionContextProvider"));
    }

    @Test
    public void testGetUrlSnapshot() throws IOException {
        JavaDocHelper helper = JavaDocHelper.getHelper(DISTRIB_NAME, "11.1-SNAPSHOT");
        // service
        check("https://community.nuxeo.com/api/nuxeo/11.1/javadoc/org/nuxeo/ecm/core/api/repository/RepositoryManager.html",
                helper.getUrl("org.nuxeo.ecm.core.api.repository.RepositoryManager"));
        // operation
        check("https://community.nuxeo.com/api/nuxeo/11.1/javadoc/org/nuxeo/ecm/automation/core/operations/document/UpdateDocument.html",
                helper.getUrl("org.nuxeo.ecm.automation.core.operations.document.UpdateDocument"));
        // Seam component
        check("https://community.nuxeo.com/api/nuxeo/11.1/javadoc/org/nuxeo/ecm/webapp/action/ActionContextProvider.html",
                helper.getUrl("org.nuxeo.ecm.webapp.action.ActionContextProvider"));
    }

    @Test
    public void testGetUrlLegacy() throws IOException {
        JavaDocHelper helper = JavaDocHelper.getHelper(DISTRIB_NAME, "5.8");
        // service
        check("https://community.nuxeo.com/api/nuxeo/release-5.8/javadoc/org/nuxeo/ecm/core/api/repository/RepositoryManager.html",
                helper.getUrl("org.nuxeo.ecm.core.api.repository.RepositoryManager"));
        // operation
        check("https://community.nuxeo.com/api/nuxeo/release-5.8/javadoc/org/nuxeo/ecm/automation/core/operations/document/UpdateDocument.html",
                helper.getUrl("org.nuxeo.ecm.automation.core.operations.document.UpdateDocument"));
        // Seam component
        check("https://community.nuxeo.com/api/nuxeo/release-5.8/javadoc/org/nuxeo/ecm/webapp/action/ActionContextProvider.html",
                helper.getUrl("org.nuxeo.ecm.webapp.action.ActionContextProvider"));
        // old use case (service removed in versions >= 10.10)
        check("https://community.nuxeo.com/api/nuxeo/release-5.8/javadoc/org/nuxeo/dam/DamService.html",
                helper.getUrl("org.nuxeo.dam.DamService"));
    }

}
