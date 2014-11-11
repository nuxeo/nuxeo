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

package org.nuxeo.ecm.platform.userdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class UserDataService extends DefaultComponent {

    public static final String NAME = UserDataService.class.getCanonicalName();

    private static final Log log = LogFactory.getLog(UserDataService.class);

    private UserDataManager manager;


    @Override
    public void activate(ComponentContext context) throws Exception {
        log.debug("<activate>");
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        log.debug("<deactivate>");
        super.deactivate(context);
    }

    public UserDataManager getManager() {
        if (manager == null) {
            manager = new UserDataManager();
        }
        return manager;
    }

}
