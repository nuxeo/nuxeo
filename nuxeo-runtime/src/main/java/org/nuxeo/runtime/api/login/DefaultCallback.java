/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
