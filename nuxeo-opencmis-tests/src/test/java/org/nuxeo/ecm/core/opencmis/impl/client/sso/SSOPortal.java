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
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoPortalSSOAuthenticationProvider;

public class SSOPortal extends AbstractClientSupport {

    protected final String secret;

    public SSOPortal(String location, String secret) {
        super(location);
        this.secret = secret;
    }

    @Override
    protected void injectParameters() {
        super.injectParameters();
        params.put(NuxeoPortalSSOAuthenticationProvider.SECRET_KEY, secret);
        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, NuxeoPortalSSOAuthenticationProvider.class.getName()); // to
                                                                                                                          // //
                                                                                                                          // server.
        params.put(SessionParameter.USER, "Administrator");
    }

    public static void main(String args[]) {
        SSOPortal client = new SSOPortal("http://localhost:8080/nuxeo/atom/cmis", "nuxeo5secretkey");
        Session session = client.connect();
        CmisObject root = session.getRootFolder();
        for (Property<?> prop : root.getProperties()) {
            String msg = String.format("%s=%s", prop.getDisplayName(), prop.getValueAsString());
            System.out.println(msg);
        }
    }

}
