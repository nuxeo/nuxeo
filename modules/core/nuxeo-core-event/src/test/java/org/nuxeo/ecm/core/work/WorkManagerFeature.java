/*
 * (C) Copyright 2018-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.work;

import static org.nuxeo.common.test.logging.NuxeoLoggingConstants.MARKER_CONSOLE_OVERRIDE;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.test.configuration.ThirdPartyUnderTest;
import org.nuxeo.common.test.configuration.ThirdPartyUnderTest.SystemProperty;
import org.nuxeo.ecm.core.event.CoreEventFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature.Waiter;

/**
 * The work manager feature provides a {@link WorkManager} implementation depending on test configuration.
 * <p>
 * To run your unit tests on a {@link StreamWorkManager} you need to declare {@code nuxeo.test.workmanager=stream} in
 * your system properties.
 *
 * @since 10.3
 */
@Deploy("org.nuxeo.runtime.kv") // for stream work manager
@Features(CoreEventFeature.class)
public class WorkManagerFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(WorkManagerFeature.class);

    public static final String BUNDLE_TEST_NAME = "org.nuxeo.ecm.core.event.test";

    public static final SystemProperty TEST_WORK_MANAGER_PROPERTY = new SystemProperty("nuxeo.test.workmanager",
            "default");

    /**
     * @deprecated since 2025.0, use {@link #TEST_WORK_MANAGER_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String WORK_MANAGER_PROPERTY = TEST_WORK_MANAGER_PROPERTY.key();

    public static final String WORK_MANAGER_DEFAULT = "default";

    public static final String WORK_MANAGER_STREAM = "stream";

    // stream work manager properties part

    public static final SystemProperty STREAM_WORK_MANAGER_STORESTATE_PROPERTY = new SystemProperty(
            "nuxeo.test.workmanager.stream.storestate.enabled", "false");

    /**
     * @deprecated since 2025.0, use {@link #STREAM_WORK_MANAGER_STORESTATE_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String STREAM_WORK_MANAGER_STORESTATE_ENABLED_PROPERTY = STREAM_WORK_MANAGER_STORESTATE_PROPERTY.key();

    /**
     * @deprecated since 2025.0, use {@link #STREAM_WORK_MANAGER_STORESTATE_PROPERTY} instead
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static final String STREAM_WORK_MANAGER_STORESTATE_ENABLED_DEFAULT = STREAM_WORK_MANAGER_STORESTATE_PROPERTY.defaultValue();

    protected String workManagerType;

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(TransactionalFeature.class).addWaiter(new WorksWaiter());
    }

    @Override
    public void start(FeaturesRunner runner) {
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        workManagerType = ThirdPartyUnderTest.computeSystemProperty(TEST_WORK_MANAGER_PROPERTY);
        try {
            log.info(MARKER_CONSOLE_OVERRIDE, "Deploying WorkManager using {} implementation", workManagerType);
            switch (workManagerType) {
                case WORK_MANAGER_DEFAULT -> initDefaultImplementation(harness);
                case WORK_MANAGER_STREAM -> initStreamImplementation(harness);
                default ->
                    throw new UnsupportedOperationException(workManagerType + " work manager type is not supported");
            }
        } catch (Exception e) {
            throw new RuntimeServiceException("Unable to configure the work manager implementation", e);
        }
    }

    protected void initDefaultImplementation(RuntimeHarness harness) throws Exception {
        harness.deployContrib(BUNDLE_TEST_NAME, "OSGI-INF/test-default-workmanager-config.xml");
    }

    protected void initStreamImplementation(RuntimeHarness harness) throws Exception {
        // compute the store state enabled property
        ThirdPartyUnderTest.computeSystemProperty(TEST_WORK_MANAGER_PROPERTY);
        harness.deployContrib(BUNDLE_TEST_NAME, "OSGI-INF/test-stream-workmanager-config.xml");
    }

    public boolean isDefault() {
        return WORK_MANAGER_DEFAULT.equals(workManagerType);
    }

    public boolean isStream() {
        return WORK_MANAGER_STREAM.equals(workManagerType);
    }

    public static class WorksWaiter implements Waiter {

        @Override
        public boolean await(Duration duration) throws InterruptedException {
            WorkManager workManager = Framework.getService(WorkManager.class);
            if (workManager.awaitCompletion(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                return true;
            }
            logInfos(workManager);
            return false;
        }

        protected void logInfos(WorkManager workManager) {
            StringBuilder sb = new StringBuilder().append("await timeout on WorkManager ");
            for (String queueId : workManager.getWorkQueueIds()) {
                sb.append(System.lineSeparator());
                sb.append(workManager.getMetrics(queueId));
            }
            log.error(sb.toString(), new Throwable("stack trace"));
        }

    }
}
