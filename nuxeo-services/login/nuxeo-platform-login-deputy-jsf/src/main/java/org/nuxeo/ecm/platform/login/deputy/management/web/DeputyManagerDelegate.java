/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.login.deputy.management.web;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.platform.login.deputy.management.DeputyManager;
import org.nuxeo.runtime.api.Framework;

@Name("deputyManager")
@Scope(SESSION)
public class DeputyManagerDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Unwrap
    public DeputyManager factoryForDeputyManager() {
        return Framework.getService(DeputyManager.class);
    }

}
