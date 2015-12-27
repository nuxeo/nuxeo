/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LDAPSubstringMatchType.java 29934 2008-02-07 12:31:10Z atchertchian $
 */

package org.nuxeo.ecm.directory.ldap;

/**
 * Substring match types: one of subany, subinitial ot subfinal.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LDAPSubstringMatchType {

    public static final String SUBINITIAL = "subinitial";

    public static final String SUBFINAL = "subfinal";

    public static final String SUBANY = "subany";

    private LDAPSubstringMatchType() {
    }

}
