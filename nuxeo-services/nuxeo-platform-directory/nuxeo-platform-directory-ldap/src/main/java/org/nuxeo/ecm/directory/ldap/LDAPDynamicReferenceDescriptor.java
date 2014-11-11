/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.directory.ldap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Dynamic Reference descriptor for Ldap References. Used to setup a dynamic
 * reference with a set of attributes instead of an url (dynamicAttributeId).
 *
 * <ul>
 * <li>filter: Attribut that contains the filter, example
 * "msExchDynamicDLFilter"</li>
 * <li>type: subtree/one</li>
 * <li>baseDN: Attribut that contains the BaseDN used for search, example
 * "msExchDynamicDLBaseDN"</li>
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
