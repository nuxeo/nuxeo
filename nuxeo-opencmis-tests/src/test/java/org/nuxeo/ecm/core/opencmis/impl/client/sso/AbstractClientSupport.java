/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin (aka matic)
 */

package org.nuxeo.ecm.core.opencmis.impl.client.sso;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.opencmis.impl.client.protocol.http.NullAuthenticationProvider;

public class AbstractClientSupport {

    protected final Map<String, String> params = new HashMap<String, String>();

    protected Session session;

    protected String location;

    protected AbstractClientSupport(String location) {
        this.location = location;
    }

    protected void injectParameters() {

        // Where to go
        params.put(SessionParameter.ATOMPUB_URL, location); // URL

        // Do not set authentication header
        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, NullAuthenticationProvider.class.getName()); // to //
                                                                                                                // server.

        // parameter.put(SessionParameter.REPOSITORY_ID, "myRepository"); //
        // Only necessary if there is more than one repository.
        params.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

        // Session locale.
        params.put(SessionParameter.LOCALE_ISO3166_COUNTRY, "");
        params.put(SessionParameter.LOCALE_ISO639_LANGUAGE, "en");
        params.put(SessionParameter.LOCALE_VARIANT, "US");

    }

    public Session connect() {
        injectParameters();
        SessionFactory factory = SessionFactoryImpl.newInstance();
        // Create session.
        try {
            // This supposes only one repository is available at the URL.
            Repository soleRepository = factory.getRepositories(params).get(0);
            return session = soleRepository.createSession();
        } catch (CmisConnectionException e) {
            // The server is unreachable
            throw new Error("Server unreachable", e);
        } catch (CmisRuntimeException e) {
            // The user/password have probably been rejected by the server.
            throw new Error("Security error ?", e);
        }
    }

}
