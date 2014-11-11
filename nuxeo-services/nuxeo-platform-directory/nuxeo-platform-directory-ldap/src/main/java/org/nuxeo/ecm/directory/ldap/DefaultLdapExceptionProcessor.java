/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.directory.ldap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Default implementation of the {@link LdapExceptionProcessor} based on Errors
 * returned by OpenDS
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 *
 */
public class DefaultLdapExceptionProcessor implements LdapExceptionProcessor {

    protected Pattern err53 = Pattern.compile(".*\\[LDAP: error code 53 - (.*)\\].*");

    public RecoverableClientException extractRecoverableException(Exception e) {

        String errMsg = e.getMessage();
        if (errMsg == null) {
            return null;
        }
        Matcher matcher53 = err53.matcher(errMsg);

        if (matcher53.matches()) {
            String userMessage = matcher53.group(1);
            return new RecoverableClientException(userMessage, userMessage,
                    null, e);
        }

        return null;
    }
}
