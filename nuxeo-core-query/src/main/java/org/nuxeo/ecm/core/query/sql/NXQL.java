/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql;

/**
 * This defines the constants for NXQL queries.
 *
 * @author Florent Guillaume
 */
public class NXQL {

    // constant utility class
    private NXQL() {
    }

    public static final String ECM_PREFIX = "ecm:";

    public static final String ECM_UUID = "ecm:uuid";

    public static final String ECM_PATH = "ecm:path";

    public static final String ECM_NAME = "ecm:name";

    public static final String ECM_PARENTID = "ecm:parentId";

    public static final String ECM_MIXINTYPE = "ecm:mixinType";

    public static final String ECM_PRIMARYTYPE = "ecm:primaryType";

    public static final String ECM_ISPROXY = "ecm:isProxy";

    public static final String ECM_ISVERSION = "ecm:isCheckedInVersion";

    public static final String ECM_LIFECYCLESTATE = "ecm:currentLifeCycleState";

    public static final String ECM_VERSIONLABEL = "ecm:versionLabel";

    public static final String ECM_FULLTEXT = "ecm:fulltext";

}
