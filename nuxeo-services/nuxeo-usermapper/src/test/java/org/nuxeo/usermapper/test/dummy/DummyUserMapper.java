/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.usermapper.test.dummy;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.extension.AbstractUserMapper;
import org.nuxeo.usermapper.extension.UserMapper;

/**
 * @author tiry
 */
public class DummyUserMapper extends AbstractUserMapper implements UserMapper {

    @Override
    public Object wrapNuxeoPrincipal(NuxeoPrincipal principal, Object userObject, Map<String, Serializable> params) {
        return null;
    }

    @Override
    public void init(Map<String, String> params) throws Exception {
    }

    @Override
    public void release() {
    }

    @Override
    protected void resolveAttributes(Object userObject, Map<String, Serializable> searchAttributes,
            Map<String, Serializable> userAttributes, Map<String, Serializable> profileAttributes) {
        if (userObject instanceof DummyUser) {
            DummyUser du = (DummyUser) userObject;
            searchAttributes.put(getUserManager().getUserIdField(), du.login);
            if (du.getName().getFirstName() != null) {
                userAttributes.put("firstName", du.getName().getFirstName());
            }
            if (du.getName().getLastName() != null) {
                userAttributes.put("lastName", du.getName().getLastName());
            }
        }
    }

}
