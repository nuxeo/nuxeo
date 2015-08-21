/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.usermapper.test.dummy;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.extension.AbstractUserMapper;
import org.nuxeo.usermapper.extension.UserMapper;

/**
 *
 * @author tiry
 *
 */
public class DummyUserMapper extends AbstractUserMapper implements UserMapper {

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal) {
        return null;
    }

    @Override
    public void init(Map<String, String> params) throws Exception {
    }

    @Override
    public void release() {
    }

    @Override
    protected void resolveAttributes(Object userObject,
            Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {
        if (userObject instanceof DummyUser) {
            DummyUser du = (DummyUser)userObject;
            searchAttributes.put(userManager.getUserIdField(), du.login);
            if (du.getName().getFirstName()!=null) {
                userAttributes.put("firstName", du.getName().getFirstName());
            }
            if (du.getName().getLastName()!=null) {
                userAttributes.put("lastName", du.getName().getLastName());
            }
        }
    }

}
