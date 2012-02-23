/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

/**
 * A simple callback handler that can be used to get authentication credentials
 * as a Java Object.
 * <p>
 * The type of object is specific to each {@link CallbackHandler} that is
 * accepting this callback.
 *
 * @author  eionica@nuxeo.com
 *
 */
public class CredentialsCallback implements Callback {

    private Object credentials;

    public CredentialsCallback() {
    }

    public Object getCredentials() {
        return credentials;
    }

    public void setCredentials(Object credentials) {
        this.credentials = credentials;
    }
    
}
