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

package org.nuxeo.scim.server.tests;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.httpclient.ApacheHttpClientConfig;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.google.inject.Inject;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.sdk.PreemptiveAuthInterceptor;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMService;

@RunWith(FeaturesRunner.class)
@Features({ ScimFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = ScimServerInit.class)
@Ignore("Unable to make it run reliability because of internal parser issues ...")
public class ScimServerTest {

    @Inject
    CoreSession session;

    @Test
    public void shouldListUsers() throws Exception {

        final URI uri = URI.create("http://localhost:18090/scim/v1/");

        final ClientConfig clientConfig = createHttpBasicClientConfig("user0", "user0");
        final SCIMService scimService = new SCIMService(uri, clientConfig);
        scimService.setAcceptType(MediaType.APPLICATION_JSON_TYPE);

        // Core user resource CRUD operation example
        final SCIMEndpoint<UserResource> endpoint = scimService.getUserEndpoint();

        Assert.assertNotNull(endpoint);

        UserResource u = endpoint.get("user0");

        Assert.assertEquals("user0", u.getId());
        Assert.assertEquals("Steve", u.getName().getGivenName());
        Assert.assertEquals("Jobs", u.getName().getFamilyName());
        Assert.assertEquals("http://localhost:18090/scim/v1/Users/user0", u.getMeta().getLocation().toString());

        List<String> actualGroups = new ArrayList<>();
        for (Entry<String> group : u.getGroups()) {
            actualGroups.add(group.getValue());
        }
        Assert.assertTrue(actualGroups.contains("powerusers"));
        Assert.assertTrue(actualGroups.contains("defgr"));
        Assert.assertEquals(2, actualGroups.size());

        Resources<UserResource> users = endpoint.query(null);
        Assert.assertEquals(5, users.getTotalResults());

        users = endpoint.query("userName eq user1");
        Assert.assertEquals(1, users.getTotalResults());
        Assert.assertEquals("user1", users.iterator().next().getId());

    }

    protected static ClientConfig createHttpBasicClientConfig(final String userName, final String password) {

        final HttpParams params = new BasicHttpParams();
        DefaultHttpClient.setDefaultHttpParams(params);
        params.setBooleanParameter(CoreConnectionPNames.SO_REUSEADDR, true);
        params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);

        final SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

        final PoolingClientConnectionManager mgr = new PoolingClientConnectionManager(schemeRegistry);
        mgr.setMaxTotal(200);
        mgr.setDefaultMaxPerRoute(20);

        final DefaultHttpClient httpClient = new DefaultHttpClient(mgr, params);

        final Credentials credentials = new UsernamePasswordCredentials(userName, password);
        httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
        httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);

        ClientConfig clientConfig = new ApacheHttpClientConfig(httpClient);
        clientConfig.setBypassHostnameVerification(true);

        return clientConfig;
    }

}
