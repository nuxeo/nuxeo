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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.http.readonly;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.http.readonly.filter.RequestValidator;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
public class TestRequestValidator {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private RequestValidator validator;

    @Before
    public void initValidator() {
        this.validator = new RequestValidator();
    }

    @Test
    public void testCheckValidDocumentId() throws Exception {
        validator.checkValidDocumentId("123");
        exception.expect(IllegalArgumentException.class);
        validator.checkValidDocumentId(null);
    }

    @Test
    public void testGetTypes() throws Exception {
        validator.getTypes("nxutest", "doc");
        validator.getTypes("nxutest", "doc,doc");
        String types = validator.getTypes("nxutest", null);
        Assert.assertEquals("doc", types);
        types = validator.getTypes("nxutest", "*");
        Assert.assertEquals("doc", types);
        types = validator.getTypes("nxutest", "_all");
        Assert.assertEquals("doc", types);
    }

    @Test
    public void testGetInvalidTypes1() {
        exception.expect(IllegalArgumentException.class);
        validator.getTypes("nxutest", "unexisting");
    }

    @Test
    public void testGetInvalidTypes2() {
        exception.expect(IllegalArgumentException.class);
        validator.getTypes("nxutest", "doc, doc");
    }

    @Test
    public void testGetIndices() throws Exception {
        validator.getIndices("nxutest");
        validator.getIndices("nxutest,nxutest");
        String indices = validator.getIndices(null);
        Assert.assertEquals("nxutest", indices);
        indices = validator.getIndices("*");
        Assert.assertEquals("nxutest", indices);
        indices = validator.getIndices("_all");
        Assert.assertEquals("nxutest", indices);
    }

    @Test
    public void testGetInvalidIndices1() {
        exception.expect(IllegalArgumentException.class);
        validator.getIndices("unexisting");
    }

    @Test
    public void testGetInvalidIndices2() {
        exception.expect(IllegalArgumentException.class);
        validator.getIndices("nxutest,unexisting");
    }

    @Test
    public void testGetInvalidIndices3() {
        exception.expect(IllegalArgumentException.class);
        validator.getIndices("?");
    }

    @Test
    public void testHasAccessAllowed() throws JSONException {
        validator.checkAccess(
                TestSearchRequestFilter.getNonAdminPrincipal(),
                "{\"_index\":\"nuxeo\",\"_type\":\"doc\",\"_id\":\"f1714dd9-ba3e-4c1a-845f-0cd2f7defd7c\",\"_version\":1,\"found\":true,\"fields\":{\"ecm:acl\":[\"Administrator\",\"members\"]}}");
    }

    @Test
    public void testHasAccessDenied() throws JSONException {
        exception.expect(SecurityException.class);
        validator.checkAccess(
                TestSearchRequestFilter.getNonAdminPrincipal(),
                "{\"_index\":\"nuxeo\",\"_type\":\"doc\",\"_id\":\"f1714dd9-ba3e-4c1a-845f-0cd2f7defd7c\",\"_version\":1,\"found\":true,\"fields\":{\"ecm:acl\":[\"Administrator\"]}}");
    }

    @Test
    public void testHasAccessNotFound() throws JSONException {
        exception.expect(SecurityException.class);
        validator.checkAccess(TestSearchRequestFilter.getNonAdminPrincipal(),
                "{\"_index\":\"nuxeo\",\"_type\":\"doc\",\"_id\":\"f1714dd9-ba3e-4c1a-845f-e0cd2f7defd7c\",\"found\":false}");
    }

}