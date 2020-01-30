/*
 * (C) Copyright 2014-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:ncunha@nuxeo.com">Nuno Cunha</a>
 *
 */
package org.nuxeo.ecm.restapi.server.jaxrs.routing;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

@RunWith(FeaturesRunner.class)
@Features({ EmbeddedAutomationServerFeature.class, WorkflowFeature.class, MockitoFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.server.routing")
@Deploy("org.nuxeo.ecm.platform.restapi.server")
@Deploy("org.nuxeo.ecm.platform.routing.default")
public class TaskObjectTest extends BaseTest {

    @Mock
    @RuntimeService
    protected DocumentRoutingService routingService;

    @Test
    public void shouldCallNonPaginatedMethodWhenNoParameter() {
        getResponse(BaseTest.RequestType.GET, "/task");
        verify(routingService).getTasks(any(DocumentModel.class), anyString(), anyString(), anyString(), any(CoreSession.class));
    }

    @Test
    public void shouldCallNonPaginatedMethodWhenParameterIsFalse() {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl() {{
            add("isPaginated", "false");
        }};
        getResponse(BaseTest.RequestType.GET, "/task", queryParams);
        verify(routingService).getTasks(any(DocumentModel.class), anyString(), anyString(), anyString(), any(CoreSession.class));
    }

    @Test
    public void shouldCallPaginatedMethodWhenParameterIsTrue() {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl() {{
            add("isPaginated", "true");
        }};
        getResponse(BaseTest.RequestType.GET, "/task", queryParams);
        verify(routingService, Mockito.times(0)).getTasks(any(DocumentModel.class), anyString(), anyString(), anyString(), any(CoreSession.class));
    }
}
