/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * CMIS client authenticated using CAS. Fetch documents using the atomPUB
 * protocol.
 *
 * The authentication outside the scope of chemistry. We've managed to direct
 * chemistry for using the authenticated http client.
 *
 * @author matic
 *
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
