/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
