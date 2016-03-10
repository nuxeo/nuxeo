/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.directory.ldap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Default implementation of the {@link LdapExceptionProcessor} based on Errors returned by OpenDS
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class DefaultLdapExceptionProcessor implements LdapExceptionProcessor {

    protected Pattern err53 = Pattern.compile(".*\\[LDAP: error code 53 - (.*)\\].*");

    @Override
    public RecoverableClientException extractRecoverableException(Exception e) {

        String errMsg = e.getMessage();
        if (errMsg == null) {
            return null;
        }
        Matcher matcher53 = err53.matcher(errMsg);

        if (matcher53.matches()) {
            String userMessage = matcher53.group(1);
            return new RecoverableClientException(userMessage, userMessage, null, e);
        }

        return null;
    }
}
