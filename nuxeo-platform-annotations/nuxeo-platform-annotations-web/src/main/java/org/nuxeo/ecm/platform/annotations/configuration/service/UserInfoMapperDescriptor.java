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
 *     qlamerand
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.configuration.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.annotations.gwt.server.configuration.UserInfoMapper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Quentin Lamerand</a>
 *
 */
@XObject("userInfoMapper")
public class UserInfoMapperDescriptor {

    @XNode("@class")
    private Class<UserInfoMapper> klass;

    public Class<UserInfoMapper> getKlass() {
        return klass;
    }

}
