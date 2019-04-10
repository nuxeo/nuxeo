/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import org.apache.chemistry.opencmis.client.api.Session;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Binder;

/**
 * Base feature that starts a CMIS client session.
 */
public abstract class CmisFeatureSession extends CmisFeatureConfiguration {

    public static final String USERNAME = "Administrator";

    public static final String PASSWORD = "test";

    protected boolean isHttp;

    protected boolean isAtomPub;

    protected boolean isBrowser;

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(CmisFeatureSession.class).toInstance(this);
    }

    public abstract Session setUpCmisSession(String repositoryName);

    public abstract void tearDownCmisSession();

    public void setLocal() {
        isHttp = false;
        isAtomPub = false;
        isBrowser = false;
    }

    public void setAtomPub() {
        isHttp = true;
        isAtomPub = true;
        isBrowser = false;
    }

    public void setBrowser() {
        isHttp = true;
        isAtomPub = false;
        isBrowser = true;
    }

    public void setWebServices() {
        isHttp = true;
        isAtomPub = false;
        isBrowser = false;
    }

}
