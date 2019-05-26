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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 * @deprecated use PermissionVisibilityDescriptor (in the core) instead
 */
@Deprecated
@XObject("uiPermissionList")
public class UIPermissionListDescriptor {

    @XNode("@documentType")
    protected String documentType;

    @XNodeList(value = "permission@name", type = String[].class, componentType = String.class)
    protected String[] permissions;

    @XNode("@default")
    protected boolean isDefault = false;

}
