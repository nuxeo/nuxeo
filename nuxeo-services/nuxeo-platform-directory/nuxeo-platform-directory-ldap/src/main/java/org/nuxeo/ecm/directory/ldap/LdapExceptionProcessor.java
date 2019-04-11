/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Interface used to provide a pluggable LDAP Exception processor
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public interface LdapExceptionProcessor {

    /**
     * Implementation should check the input Exception and turn it into a RecoverableClientException if the Exception
     * should be displayed to the end user.
     *
     * @param e
     * @return a RecoverableClientException if needed and null otherwise
     */
    RecoverableClientException extractRecoverableException(Exception e);

}
