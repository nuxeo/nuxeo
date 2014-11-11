/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.invoker;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.server.PublicationInvokationHandler;
import org.nuxeo.ecm.platform.publisher.remoting.server.TestInvokationHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Dummy test invoker: does all marshaling work but directly calls the
 * {@link TestInvokationHandler} without any network.
 *
 * @author tiry
 */
public class DefaultRemotePublicationInvoker implements
        RemotePublicationInvoker {

    protected String baseURL;

    protected String userName;

    protected String password;

    protected RemotePublisherMarshaler marshaler;

    protected PublicationInvokationHandler testPublicationHandler;

    protected boolean useTestMode = false;

    public void init(String baseURL, String userName, String password,
            RemotePublisherMarshaler marshaler) {
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

    public Object invoke(String methodName, List<Object> params)
            throws ClientException {

        String marshaledData = marshaler.marshallParameters(params);

        String result = doInvoke(methodName, marshaledData);

        if (result == null)
            return null;
        return marshaler.unMarshallResult(result);
    }

    protected String doInvoke(String methodName, String marshaledData)
            throws ClientException {

        if (useTestMode) {
            return testPublicationHandler.invoke(methodName, marshaledData);
        } else {
            if (baseURL.startsWith("http")) {
                try {
                    return doHttpCall(methodName, marshaledData);
                } catch (Exception e) {
                    throw new ClientException("Error in http communication", e);
                }
            }
            throw new ClientException("Unhandled protocol for url " + baseURL);
        }
    }

    protected String doHttpCall(String methodName, String marshaledData)
            throws ClientException, ClientProtocolException, IOException {

        HttpClient httpClient = new DefaultHttpClient();

        String BAHeaderContent = userName + ":" + password;
        BAHeaderContent = Base64.encodeBytes(BAHeaderContent.getBytes());
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
        InputStreamReader isr = new InputStreamReader(
                responseEntity.getContent(), "UTF-8");

        BufferedReader br = new BufferedReader(isr);

        StringBuffer sb = new StringBuffer();

        int ch;
        while ((ch = br.read()) > -1) {
            sb.append((char) ch);
        }
        br.close();

        String result = sb.toString();

        return result;
    }

}
