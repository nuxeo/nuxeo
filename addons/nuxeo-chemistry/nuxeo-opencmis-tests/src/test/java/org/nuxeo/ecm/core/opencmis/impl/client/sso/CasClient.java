/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.sso;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.nuxeo.ecm.core.opencmis.impl.client.protocol.http.HttpURLInstaller;
import org.nuxeo.ecm.core.opencmis.impl.client.protocol.http.NullAuthenticationProvider;

/**
 * CMIS client authenticated using CAS. Fetch documents using the atomPUB protocol. The authentication outside the scope
 * of chemistry. We've managed to direct chemistry for using the authenticated http client.
 *
 * @author matic
 */
public class CasClient extends AbstractClientSupport {

    public Cookie[] cookies;

    public CasClient(String location) {
        super(location);
    }

    public void saveClientContext() {
        HttpClient client = HttpURLInstaller.INSTANCE.getClient();
        cookies = client.getState().getCookies();
    }

    public void restoreClientContext() {
        HttpClient client = HttpURLInstaller.INSTANCE.getClient();
        HttpState state = client.getState();
        state.clearCookies();
        for (Cookie cookie : cookies) {
            state.addCookie(cookie);
        }
    }

    public CasGreeter newGreeter() {
        HttpClient client = HttpURLInstaller.INSTANCE.getClient();
        return new CasGreeter(client, location);
    }

    @Override
    protected void injectParameters() {
        super.injectParameters();
        // Do not set authentication header
        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, NullAuthenticationProvider.class.getName()); // to
                                                                                                                // //
                                                                                                                // server.
    }

    public static void main(String args[]) throws Exception {

        HttpURLInstaller.install();

        CasClient client = new CasClient("http://127.0.0.1:8080/nuxeo/atom/cmis");

        String ticketGranting = client.newGreeter().credsLogon("slacoin", "slacoin");

        assert ticketGranting != null;

        // client.client = installClient();
        //
        // String secondTicket = client.logon(firstTicket);
        //
        // assert firstTicket.equals(secondTicket);

        Session session = client.connect();
        CmisObject root = session.getRootFolder();

        for (Property<?> prop : root.getProperties()) {
            String msg = String.format("%s=%s", prop.getDisplayName(), prop.getValueAsString());
            System.out.println(msg);
        }
    }

}
