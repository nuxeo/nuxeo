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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.functionaltests.RestHelper;

/**
 * This Rule gives mechanism for hot reload tests.
 * <p />
 * This rule will deploy the dev bundle located under src/test/resources/${YOUR_TEST_CLASS_NAME}/${YOUR_TEST_NAME} (if
 * exists) before the test starts, and undeploy it when test finish.
 * <p />
 * In order to use this rule you need to add these properties to your nuxeo.conf:
 * <ul>
 * <li>nuxeo.templates=default,sdk (just add the sdk template)</li>
 * <li>org.nuxeo.dev=true</li>
 * <li>nuxeo.server.sdkInstallReloadTimer=false</li>
 * </ul>
 * 
 * @since 9.3
 */
public class HotReloadTestRule implements TestRule {

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
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                starting(description);
                try {
                    base.evaluate();
                } finally {
                    finished(description);
                }
            }

        };
    }

    protected void starting(Description description) {
        RestHelper.logOnServer(String.format("Starting test '%s#%s'", description.getTestClass().getSimpleName(),
                description.getMethodName()));
        deployDevBundle(description);
    }

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
    @SuppressWarnings("ConstantConditions")
    protected void deployDevBundle(Description description) {
        Class<?> testClass = description.getTestClass();
        // first lookup the absolute paths
        String className = testClass.getSimpleName();
        String methodName = description.getMethodName();
        String relativeBundlePath = "/" + className + "/" + methodName;
        // #getResource could return null if resource doesn't exist
        Optional<Path> bundlePathOpt = Optional.ofNullable(testClass.getResource(relativeBundlePath))
                                              .map(URI_MAPPER)
                                              .map(Paths::get)
                                              .map(Path::toAbsolutePath)
                                              .filter(p -> p.toFile().exists());
        if (bundlePathOpt.isPresent()) {
            Path bundlePath = bundlePathOpt.get();
            Function<String, String> devBundleFormat = "Bundle:"::concat;
            if (bundlePath.resolve("META-INF").toFile().exists()) {
                // single bundle deployment
                updateDevBundles(devBundleFormat.apply(bundlePath.toString()));
            } else {
                // multiple bundles deployment
                String body = Stream.of(bundlePath.toFile().list())
                                    .map(bundlePath.toString().concat("/")::concat)
                                    .map(devBundleFormat)
                                    .collect(Collectors.joining(System.lineSeparator()));
                updateDevBundles(body);
            }
        } else {
            log.info(String.format("No bundle to deploy for %s#%s at path=%s", className, methodName,
                    relativeBundlePath));
        }
    }

    /**
     * Updates the dev.bundles file by POSTing a body to {@link org.nuxeo.runtime.tomcat.dev.DevValve}, this will
     * trigger a hot reload on server.
     * <p />
     * We do a HTTP POST to /sdk/reload, then wait until Nuxeo server finished to reload.
     * <p />
     * This method could throw a {@link java.net.SocketTimeoutException} if server takes more than the timeout set on
     * java client, see {@link RestHelper#CLIENT}. Furthermore, test will fail if server returns an error.
     */
    public void updateDevBundles(String body) {
        // we don't want any aync work to still be running during hot-reload as for now
        // it may cause spurious exception in the logs (NXP-23286)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timeoutSecond", Integer.valueOf(110));
        parameters.put("waitForAudit", Boolean.TRUE);
        RestHelper.operation("Elasticsearch.WaitForIndexing", parameters);

        // POST new dev bundles to deploy
        if (!RestHelper.post(NUXEO_RELOAD_PATH, body)) {
            fail("Unable to reload dev bundles, for body=" + body);
        }
    }

}
