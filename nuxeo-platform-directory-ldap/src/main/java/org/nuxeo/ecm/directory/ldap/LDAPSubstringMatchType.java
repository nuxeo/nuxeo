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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LDAPSubstringMatchType.java 29934 2008-02-07 12:31:10Z atchertchian $
 */

package org.nuxeo.ecm.directory.ldap;

/**
 * Substring match types: one of subany, subinitial ot subfinal.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class LDAPSubstringMatchType {

    public static final String SUBINITIAL = "subinitial";

    public static final String SUBFINAL = "subfinal";

    public static final String SUBANY = "subany";

    private LDAPSubstringMatchType() {
    }

}
