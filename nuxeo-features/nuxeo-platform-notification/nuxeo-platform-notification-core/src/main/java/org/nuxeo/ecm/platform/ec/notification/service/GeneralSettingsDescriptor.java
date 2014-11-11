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

package org.nuxeo.ecm.platform.ec.notification.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 *
 */
@XObject("settings")
public class GeneralSettingsDescriptor {

    @XNode("serverPrefix")
    protected String serverPrefix;

    @XNode("eMailSubjectPrefix")
    protected String eMailSubjectPrefix;

    @XNode("mailSessionJndiName")
    protected String mailSessionJndiName;

    public String getEMailSubjectPrefix() {
        return eMailSubjectPrefix;
    }

    public String getServerPrefix() {
        return serverPrefix;
    }

    public String getMailSessionJndiName() {
        return mailSessionJndiName;
    }

}
