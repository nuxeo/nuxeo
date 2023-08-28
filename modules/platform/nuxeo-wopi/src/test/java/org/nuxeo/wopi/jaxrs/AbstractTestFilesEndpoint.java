/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.wopi.jaxrs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ_WRITE;
import static org.nuxeo.ecm.jwt.JWTClaims.CLAIM_SUBJECT;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN_PARAMETER;
import static org.nuxeo.wopi.TestConstants.FILES_FIRST_FILE_PROPERTY;
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.jwt.JWTService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.JerseyClientHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.WOPIDiscoveryFeature;
import org.nuxeo.wopi.WOPIFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @since 2021.40
 */
@RunWith(FeaturesRunner.class)
@Features({ WOPIFeature.class, WOPIDiscoveryFeature.class, WebEngineFeature.class })
@Deploy("org.nuxeo.ecm.jwt")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-jwt-contrib.xml")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-webengine-servletcontainer-contrib.xml")
@WithFrameworkProperty(name = Environment.PRODUCT_NAME, value = "WOPI Test")
public abstract class AbstractTestFilesEndpoint {

    public static final String WOPI_FILES = "site/wopi/files";

    public static final String CONTENTS_PATH = "contents";

    @Inject
    protected UserManager userManager;

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected JWTService jwtService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected Client client;

    protected String joeToken;

    protected String johnToken;

    protected DocumentModel blobDoc;

    protected String blobDocFileId;

    protected DocumentModel zeroLengthBlobDoc;

    protected String zeroLengthBlobDocFileId;

    protected DocumentModel hugeBlobDoc;

    protected String hugeBlobDocFileId;

    protected DocumentModel noBlobDoc;

    protected String noBlobDocFileId;

    protected DocumentModel multipleBlobsDoc;

    protected String multipleBlobsDocFileId;

    protected String multipleBlobsDocAttachmentId;

    protected Blob expectedFileBlob;

    protected Blob expectedAttachmentBlob;

    ObjectMapper mapper;

    @Before
    public void setUp() throws IOException {
        mapper = new ObjectMapper();

        createUsers();

        createDocuments();

        // initialize REST API clients
        joeToken = jwtService.newBuilder().withClaim(CLAIM_SUBJECT, "joe").build();
        johnToken = jwtService.newBuilder().withClaim(CLAIM_SUBJECT, "john").build();
        client = JerseyClientHelper.clientBuilder().build();

        // make sure everything is committed
        transactionalFeature.nextTransaction();
    }

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port + "/";
    }

    protected void createUsers() {
        DocumentModel joe = userManager.getBareUserModel();
        joe.setPropertyValue("user:username", "joe");
        joe.setPropertyValue("user:password", "joe");
        joe.setPropertyValue("user:firstName", "Joe");
        joe.setPropertyValue("user:lastName", "Jackson");
        userManager.createUser(joe);

        DocumentModel john = userManager.getBareUserModel();
        john.setPropertyValue("user:username", "john");
        john.setPropertyValue("user:password", "john");
        john.setPropertyValue("user:firstName", "John");
        john.setPropertyValue("user:lastName", "Doe");
        userManager.createUser(john);
    }

    @SuppressWarnings("unchecked")
    protected void createDocuments() throws IOException {
        DocumentModel folder = session.createDocumentModel("/", "wopi", "Folder");
        folder = session.createDocument(folder);
        Map<String, String> userPermissions = new HashMap<>();
        userPermissions.put("john", READ_WRITE);
        userPermissions.put("joe", READ);
        setPermissions(folder, userPermissions);

        expectedFileBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.docx"));
        expectedAttachmentBlob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-attachment.xlsx"));

        CoreSession johnSession = coreFeature.getCoreSession("john");
        blobDoc = johnSession.createDocumentModel("/wopi", "blobDoc", "File");
        blobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) expectedFileBlob);
        blobDoc = johnSession.createDocument(blobDoc);
        blobDocFileId = FileInfo.computeFileId(blobDoc, FILE_CONTENT_PROPERTY);

        zeroLengthBlobDoc = johnSession.createDocumentModel("/wopi", "zeroLengthBlobDoc", "File");
        Blob zeroLengthBlob = Blobs.createBlob("");
        zeroLengthBlob.setFilename("zero-length-blob");
        zeroLengthBlobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) zeroLengthBlob);
        zeroLengthBlobDoc = johnSession.createDocument(zeroLengthBlobDoc);
        zeroLengthBlobDocFileId = FileInfo.computeFileId(zeroLengthBlobDoc, FILE_CONTENT_PROPERTY);

        hugeBlobDoc = johnSession.createDocumentModel("/wopi", "hugeBlobDoc", "File");
        Blob hugeBlob = mock(Blob.class, withSettings().serializable());
        Mockito.when(hugeBlob.getLength()).thenReturn(Long.MAX_VALUE);
        Mockito.when(hugeBlob.getStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
        Mockito.when(hugeBlob.getFilename()).thenReturn("hugeBlobFilename");
        hugeBlobDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) hugeBlob);
        hugeBlobDoc = johnSession.createDocument(hugeBlobDoc);
        hugeBlobDocFileId = FileInfo.computeFileId(hugeBlobDoc, FILE_CONTENT_PROPERTY);

        noBlobDoc = johnSession.createDocumentModel("/wopi", "noBlobDoc", "File");
        noBlobDoc = johnSession.createDocument(noBlobDoc);
        noBlobDocFileId = FileInfo.computeFileId(noBlobDoc, FILE_CONTENT_PROPERTY);

        multipleBlobsDoc = johnSession.createDocumentModel("/wopi", "multipleBlobsDoc", "File");
        multipleBlobsDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) expectedFileBlob);
        List<Map<String, Serializable>> files = Collections.singletonList(
                Collections.singletonMap("file", (Serializable) expectedAttachmentBlob));
        multipleBlobsDoc.setPropertyValue("files:files", (Serializable) files);
        multipleBlobsDoc = johnSession.createDocument(multipleBlobsDoc);
        expectedAttachmentBlob = (Blob) files.get(0).get("file");

        multipleBlobsDocFileId = FileInfo.computeFileId(multipleBlobsDoc, FILE_CONTENT_PROPERTY);
        multipleBlobsDocAttachmentId = FileInfo.computeFileId(multipleBlobsDoc, FILES_FIRST_FILE_PROPERTY);
    }

    protected void setPermissions(DocumentModel doc, Map<String, String> userPermissions) {
        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        userPermissions.forEach((user, permission) -> localACL.add(new ACE(user, permission, true)));
        doc.setACP(acp, true);
    }

    @After
    public void tearDown() {
        Stream.of("john", "joe").forEach(userManager::deleteUser);
        client.destroy();
    }

    protected CloseableClientResponse get(String token, String... path) {
        return get(token, null, path);
    }

    protected CloseableClientResponse get(String token, Map<String, String> headers, String... path) {
        WebResource wr = client.resource(getBaseURL() + WOPI_FILES)
                               .path(String.join("/", path))
                               .queryParam(ACCESS_TOKEN_PARAMETER, token);
        WebResource.Builder builder = wr.getRequestBuilder();
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return CloseableClientResponse.of(builder.get(ClientResponse.class));
    }

    protected CloseableClientResponse post(String token, Map<String, String> headers, String... path) {
        return post(token, null, headers, path);
    }

    protected CloseableClientResponse post(String token, String data, Map<String, String> headers, String... path) {
        WebResource wr = client.resource(getBaseURL() + WOPI_FILES)
                               .path(String.join("/", path))
                               .queryParam(ACCESS_TOKEN_PARAMETER, token);
        WebResource.Builder builder = wr.getRequestBuilder();
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return CloseableClientResponse.of(
                data != null ? builder.post(ClientResponse.class, data) : builder.post(ClientResponse.class));
    }

}
