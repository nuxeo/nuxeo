/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.nuxeo.ecm.core.api.RecoverableClientException;

/**
 * Interface used to provide a pluggable LDAP Exception processor
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public interface LdapExceptionProcessor {

    /**
     * Implementation should check the input Exception and turn it into a
     * RecoverableClientException if the Exception should be displayed to the
     * end user.
     * 
     * @param e
     * @return a RecoverableClientException if needed and null otherwise
     */
    RecoverableClientException extractRecoverableException(Exception e);

}