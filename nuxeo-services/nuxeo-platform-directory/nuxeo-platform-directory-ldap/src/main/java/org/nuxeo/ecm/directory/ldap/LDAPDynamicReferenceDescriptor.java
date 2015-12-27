/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.ldap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Dynamic Reference descriptor for Ldap References. Used to setup a dynamic reference with a set of attributes instead
 * of an url (dynamicAttributeId).
 * <ul>
 * <li>filter: Attribut that contains the filter, example "msExchDynamicDLFilter"</li>
 * <li>type: subtree/one</li>
 * <li>baseDN: Attribut that contains the BaseDN used for search, example "msExchDynamicDLBaseDN"</li>
 * </ul>
 *
 * @author <a href="mailto:nulrich@nuxeo.com">Nicolas Ulrich</a>
 */
@XObject(value = "dynamicReference")
public class LDAPDynamicReferenceDescriptor {

    @XNode("@filter")
    public String filter;

    @XNode("@baseDN")
    public String baseDN;

    @XNode("@type")
    public String type;

}
