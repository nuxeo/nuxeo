/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ftest.server.hotreload;

import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.functionaltests.RestHelper;

/**
 * This Rule gives mechanism for hot reload tests.
 * 
 * @since 9.3
 */
public class HotReloadTestRule extends TestWatcher {

    public static final String NUXEO_RELOAD_PATH = "/sdk/reload";

    private static final Log log = LogFactory.getLog(HotReloadTestRule.class);

    protected final static Function<URL, URI> URI_MAPPER = url -> {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new NuxeoException("Unable to map the url to uri", e);
        }
    };

    @Override
    protected void starting(Description description) {
        RestHelper.logOnServer(String.format("Starting test '%s#%s'", description.getTestClass().getSimpleName(),
                description.getMethodName()));
        deployDevBundle(description);
    }

    @Override
    protected void finished(Description description) {
        String className = description.getTestClass().getSimpleName();
        String methodName = description.getMethodName();
        RestHelper.logOnServer(String.format("Ending test '%s#%s'", className, methodName));
        RestHelper.cleanup();
        // reset dev.bundles file
        updateDevBundles("# AFTER TEST: " + methodName);
        RestHelper.logOnServer(String.format("Test ended '%s#%s'", className, methodName));
    }

    /**
     * Deploys the dev bundle located under src/test/resources/${YOUR_TEST_CLASS_NAME}/${YOUR_TEST_NAME} (if exists).
     * <p />
     * This method use {@link #updateDevBundles(String)}.
     */
    protected void deployDevBundle(Description description) {
        Class<?> testClass = description.getTestClass();
        // first lookup the absolute paths
        String className = testClass.getSimpleName();
        String methodName = description.getMethodName();
        String relativeBundlePath = "/" + className + "/" + methodName;
        // #getResource could return null if resource doesn't exist
        Optional<String> bundlePath = Optional.ofNullable(testClass.getResource(relativeBundlePath))
                                              .map(URI_MAPPER)
                                              .map(Paths::get)
                                              .map(Path::toAbsolutePath)
                                              .filter(p -> p.toFile().exists())
                                              .map(Path::toString);
        if (bundlePath.isPresent()) {
            updateDevBundles("Bundle:" + bundlePath.get());
        } else {
            log.info(String.format("No bundle to deploy for %s#%s at path=%s", className, methodName,
                    relativeBundlePath));
        }
    }

    /**
     * Updates the dev.bundles file by POSTing a new line to {@link org.nuxeo.runtime.tomcat.dev.DevValve}, this will
     * trigger a hot reload on server.
     * <p />
     * We do a HTTP POST to /sdk/reload, then wait until Nuxeo server finished to reload.
     * <p />
     * This method could throw a {@link java.net.SocketTimeoutException} if server takes more than the timeout set on
     * java client, see {@link RestHelper#CLIENT}. Furthermore, test will fail if server return an error.
     * <p />
     * In order to use this method you need to add these properties to your nuxeo.conf:
     * <ul>
     * <li>nuxeo.templates=default,sql,sdk (just add the sdk template)</li>
     * <li>org.nuxeo.dev=true</li>
     * <li>nuxeo.server.sdkInstallReloadTimer=false</li>
     * </ul>
     */
    public void updateDevBundles(String line) {
        // POST new dev bundles to deploy
        if (!RestHelper.post(NUXEO_RELOAD_PATH, line)) {
            fail("Unable to reload dev bundles, for line=" + line);
        }
    }

}
