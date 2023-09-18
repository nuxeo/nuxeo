/*
 * (C) Copyright 2015-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.http.readonly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.elasticsearch.http.readonly.filter.DefaultSearchRequestFilter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestSearchRequestFilter {

    private static final String INDICES = "nxutest";

    protected static final String MATCH_ALL_PAYLOAD = minifyPayload("""
            {
              "query": {
                "match_all": {}
              }
            }""");

    @Test
    public void testMatchAll() {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getNonAdminCoreSession(), INDICES, "pretty", MATCH_ALL_PAYLOAD);
        assertEquals("/nxutest/_search?pretty", filter.getUrl());
        assertEquals(minifyPayload("""
                {
                  "query": {
                    "bool": {
                      "filter": {
                        "terms": {
                          "ecm:acl": [
                            "group1",
                            "group2",
                            "members",
                            "jdoe",
                            "Everyone"
                          ]
                        }
                      },
                      "must": {
                        "match_all": {}
                      }
                    }
                  }
                }
                """), filter.getPayload());
    }

    @Test
    public void testMatchAllAsAdmin() {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getAdminCoreSession(), INDICES, "pretty", MATCH_ALL_PAYLOAD);
        assertEquals("/nxutest/_search?pretty", filter.getUrl());
        assertEquals(MATCH_ALL_PAYLOAD, filter.getPayload());
    }

    @Test
    public void testUriSearch() {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getNonAdminCoreSession(), INDICES, "size=2&q=dc%5C%3Atitle:Workspaces", null);
        assertEquals(filter.getUrl(), "/nxutest/_search?size=2");
        assertEquals(minifyPayload("""
                {
                  "query": {
                    "bool": {
                      "filter": {
                        "terms": {
                          "ecm:acl": [
                            "group1",
                            "group2",
                            "members",
                            "jdoe",
                            "Everyone"
                          ]
                        }
                      },
                      "must": {
                        "query_string": {
                          "default_operator": "OR",
                          "query": "dc\\:title:Workspaces",
                          "default_field": "_all"
                        }
                      }
                    }
                  }
                }"""), filter.getPayload());
    }

    @Test
    public void testUriSearchWithDefaultFieldAndOperator() {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getNonAdminCoreSession(), INDICES,
                "q=dc\\:title:Workspaces&pretty&df=dc:title&default_operator=AND", null);
        assertEquals(filter.getUrl(), "/nxutest/_search?pretty");
        assertEquals(minifyPayload("""
                {
                  "query": {
                    "bool": {
                      "filter": {
                        "terms": {
                          "ecm:acl": [
                            "group1",
                            "group2",
                            "members",
                            "jdoe",
                            "Everyone"
                          ]
                        }
                      },
                      "must": {
                        "query_string": {
                          "default_operator": "AND",
                          "query": "dc\\:title:Workspaces",
                          "default_field": "dc:title"
                        }
                      }
                    }
                  }
                }"""), filter.getPayload());
    }

    @Test
    public void testUriSearchAsAdmin() {
        DefaultSearchRequestFilter filter = new DefaultSearchRequestFilter();
        filter.init(getAdminCoreSession(), INDICES, "size=2&q=dc\\:title:Workspaces", null);
        assertEquals("/nxutest/_search?size=2&q=dc\\:title:Workspaces", filter.getUrl());
        assertNull(filter.getPayload());
    }

    /**
     * @since 7.4
     */
    public static CoreSession getAdminCoreSession() {
        CoreSession session = mock(CoreSession.class);
        when(session.getPrincipal()).thenReturn(getAdminPrincipal());
        return session;
    }

    /**
     * @since 7.4
     */
    public static CoreSession getNonAdminCoreSession() {
        CoreSession session = mock(CoreSession.class);
        when(session.getPrincipal()).thenReturn(getNonAdminPrincipal());
        return session;
    }

    public static NuxeoPrincipal getAdminPrincipal() {
        var principal = new UserPrincipal("admin", List.of("group1", "group2", "members"), false, true);
        principal.setFirstName("John");
        principal.setLastName("Doe Admin");
        return principal;
    }

    public static NuxeoPrincipal getNonAdminPrincipal() {
        var principal = new UserPrincipal("jdoe", List.of("group1", "group2", "members"), false, false);
        principal.setFirstName("John");
        principal.setLastName("Doe");
        return principal;
    }

    @SuppressWarnings("EscapedSpace")
    protected static String minifyPayload(String payload) {
        return payload.replaceAll("\s*\n*", "");
    }
}
