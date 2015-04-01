/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

package org.nuxeo.ecm.webapp.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;


public class UpdateUserUnrestricted extends UnrestrictedSessionRunner {

	public static final Log log = LogFactory
			.getLog(UpdateUserUnrestricted.class);


	private DocumentModel updatedUser;

	public UpdateUserUnrestricted(String defaultRepositoryName, DocumentModel userDoc) {
		super(defaultRepositoryName);
		this.updatedUser = userDoc;
	}



	@Override
	public void run() throws ClientException {

		UserManager userManager = null;
		try {
			userManager = Framework.getService(UserManager.class);
		} catch (Exception e) {
			log.error("Could not find UserManager service.", e);
			throw e;
		}

		userManager.updateUser(updatedUser);

	}
}