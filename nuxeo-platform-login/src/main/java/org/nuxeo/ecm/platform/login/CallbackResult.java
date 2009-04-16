/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.login;

import java.security.Principal;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

/**
 * Simple class to store the result of a JAAS callback
 *
 * @author tiry
 */
public class CallbackResult {

    public boolean cb_handled;

    public UserIdentificationInfo userIdent;

    public Principal principal;

    public Object credential;

}
