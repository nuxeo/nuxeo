/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Wojciech Sulejman
 *
 */
package org.nuxeo.ecm.platform.signature.web.sign;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.runtime.api.Framework;

/**
 * UserInfo service provider
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */

@Name("cUserService")
@Scope(SESSION)
public class CUserServiceBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 3L;

    @Unwrap
    public CUserService getService() {
        return Framework.getService(CUserService.class);
    }

}
