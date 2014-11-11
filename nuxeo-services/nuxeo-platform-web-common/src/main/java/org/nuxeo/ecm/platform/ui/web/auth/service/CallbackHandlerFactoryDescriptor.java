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

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoCallbackHandlerFactory;

@XObject(value = "callbackHandlerFactory")
public class CallbackHandlerFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 237654398643289764L;

    @XNode("@name")
    private String name;

    @XNode("@class")
    Class<NuxeoCallbackHandlerFactory> className;

    public Class<NuxeoCallbackHandlerFactory> getClassName() {
        return className;
    }

}
