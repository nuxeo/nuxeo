/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * Copied from jbossx.
 * <p>
 * An implementation of Callback that simply obtains an Object to be used
 * as the authentication credential. Interpretation of the Object is up to
 * the LoginModules that validate the credential.
 *
 * @author  Scott.Stark@jboss.org
 */
public class DefaultCallback implements Callback {

    private final String prompt;

    private Object credential;

    public DefaultCallback() {
        this("");
    }

    public DefaultCallback(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public Object getCredential() {
        return credential;
    }

    public void setCredential(Object credential) {
        this.credential = credential;
    }

    public void clearCredential() {
        credential = null;
    }

}
