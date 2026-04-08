/*
 * (C) Copyright 2026-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.automation.server.test;

import static junit.framework.TestCase.assertEquals;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SEE_OTHER;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.automation.server.test.AsyncOperationAdapterTest.MAPPER;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.automation.server.test.operations.GenerateBlobOperation;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @since 2025.18
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, WebEngineFeature.class })
@Deploy("org.nuxeo.ecm.automation.test")
@Deploy("org.nuxeo.ecm.automation.test:operation-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class AsyncOperationAdapterSecurityTest {

    protected static final Duration ASYNC_TASK_DURATION = Duration.ofMillis(300);

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Inject
    protected UserManager userManager;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder()
                                                                   .url(() -> servletContainerFeature.getHttpUrl()
                                                                           + "/automation")
                                                                   .adminCredentials()
                                                                   .redirectsEnabled(false)
                                                                   .build();

    @Inject
    public TransactionalFeature txFeature;

    protected String executionId;

    @Before
    public void setup() throws JsonProcessingException {
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setPropertyValue("user:username", "jdoe");
        userModel.setPropertyValue("user:password", "pass");
        userManager.createUser(userModel);
        txFeature.nextTransaction();
        executionId = initAsyncOperation();
    }

    @After
    public void teardown() {
        waitForAsyncTaskCompletion();
    }

    @Test
    public void shouldAsyncStatusAndResultBeVisibleOnlyByInitiator() throws Exception {
        // Status is not available for other user
        httpClient.buildGetRequest("/" + GenerateBlobOperation.ID + "/@async/" + executionId + "/status")
                  .credentials("jdoe", "pass")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));

        waitForAsyncTaskCompletion();

        // Result is not available for other user
        httpClient.buildGetRequest("/" + GenerateBlobOperation.ID + "/@async/" + executionId)
                  .credentials("jdoe", "pass")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
        // Result is available for initiator
        httpClient.buildGetRequest("/" + GenerateBlobOperation.ID + "/@async/" + executionId)
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

    }

    @Test
    public void shouldAsyncOperationBeAbortedOnlyByInitiator() throws Exception {
        // Other user cannot abort
        httpClient.buildDeleteRequest("/" + GenerateBlobOperation.ID + "/@async/" + executionId)
                  .credentials("jdoe", "pass")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));

        // Initiator can abort
        httpClient.buildDeleteRequest("/" + GenerateBlobOperation.ID + "/@async/" + executionId)
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

    }

    protected String initAsyncOperation() throws JsonProcessingException {
        String executionId = httpClient.buildPostRequest("/" + GenerateBlobOperation.ID + "/@async")
                                       .contentType("application/json")
                                       .entity(MAPPER.writeValueAsString(Map.of("params",
                                               Map.of("delay", DurationUtils.format(ASYNC_TASK_DURATION)))))
                                       .executeAndThen(response -> {
                                           var statusUrl = response.getLocation().toString();
                                           assertFalse(statusUrl.isBlank());
                                           String[] segments = statusUrl.split("/");
                                           // segments: [..., "GenerateBlob", "@async", "{executionId}", "status"]
                                           return segments[segments.length - 2];
                                       });
        assertTrue(Strings.isNotBlank(executionId));
        return executionId;
    }

    protected void waitForAsyncTaskCompletion() {
        await().atMost(ASYNC_TASK_DURATION.plusSeconds(1))
               .pollInterval(Duration.ofMillis(200))
               .until(() -> httpClient.buildGetRequest(
                       "/" + GenerateBlobOperation.ID + "/@async/" + executionId + "/status")
                                      .executeAndThen(response -> response.getStatus() == SC_SEE_OTHER));
    }
}
