/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.publisher.remoting.invoker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.server.PublicationInvokationHandler;
import org.nuxeo.ecm.platform.publisher.remoting.server.TestInvokationHandler;

/**
 * Dummy test invoker: does all marshaling work but directly calls the {@link TestInvokationHandler} without any
 * network.
 *
 * @author tiry
 */
public class DefaultRemotePublicationInvoker implements RemotePublicationInvoker {

    protected String baseURL;

    protected String userName;

    protected String password;

    protected RemotePublisherMarshaler marshaler;

    protected PublicationInvokationHandler testPublicationHandler;

    protected boolean useTestMode = false;

    public void init(String baseURL, String userName, String password, RemotePublisherMarshaler marshaler) {
        this.baseURL = baseURL;
        this.userName = userName;
        this.password = password;
        this.marshaler = marshaler;

        if (baseURL.startsWith("test")) {
            useTestMode = true;
            testPublicationHandler = new TestInvokationHandler(marshaler);
        } else {
            useTestMode = false;
        }
    }

    public Object invoke(String methodName, List<Object> params) {

        String marshaledData = marshaler.marshallParameters(params);

        String result = doInvoke(methodName, marshaledData);

        if (result == null)
            return null;
        return marshaler.unMarshallResult(result);
    }

    protected String doInvoke(String methodName, String marshaledData) {

        if (useTestMode) {
            return testPublicationHandler.invoke(methodName, marshaledData);
        } else {
            if (baseURL.startsWith("http")) {
                try {
                    return doHttpCall(methodName, marshaledData);
                } catch (IOException e) {
                    throw new NuxeoException("Error in http communication", e);
                }
            }
            throw new NuxeoException("Unhandled protocol for url " + baseURL);
        }
    }

    protected String doHttpCall(String methodName, String marshaledData) throws IOException {

        HttpClient httpClient = new DefaultHttpClient();

        String BAHeaderContent = userName + ":" + password;
        BAHeaderContent = Base64.encodeBase64String(BAHeaderContent.getBytes());
        String BAHeader = "basic " + BAHeaderContent;

        String targetUrl = baseURL + methodName;

        HttpPost httpPost = new HttpPost(targetUrl);

        HttpEntity entity = new StringEntity(marshaledData, "UTF-8");

        httpPost.setEntity(entity);

        httpPost.setHeader("content-type", "nuxeo/remotepub");
        httpPost.setHeader("authorization", BAHeader);

        HttpResponse response = httpClient.execute(httpPost);

        HttpEntity responseEntity = response.getEntity();
        if (responseEntity == null) {
            return null;
        }
        InputStreamReader isr = new InputStreamReader(responseEntity.getContent(), "UTF-8");

        BufferedReader br = new BufferedReader(isr);

        StringBuilder sb = new StringBuilder();

        int ch;
        while ((ch = br.read()) > -1) {
            sb.append((char) ch);
        }
        br.close();

        String result = sb.toString();

        return result;
    }

}
