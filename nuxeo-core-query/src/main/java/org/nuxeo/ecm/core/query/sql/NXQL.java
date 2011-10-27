/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    /** The NXQL query type. */
    public static final String NXQL = "NXQL";

    public static final String ECM_PREFIX = "ecm:";

    public static final String ECM_UUID = "ecm:uuid";

    public static final String ECM_PATH = "ecm:path";

    public static final String ECM_NAME = "ecm:name";

    public static final String ECM_POS = "ecm:pos";

    public static final String ECM_PARENTID = "ecm:parentId";

    public static final String ECM_MIXINTYPE = "ecm:mixinType";

    public static final String ECM_PRIMARYTYPE = "ecm:primaryType";

    public static final String ECM_ISPROXY = "ecm:isProxy";

    public static final String ECM_ISVERSION = "ecm:isCheckedInVersion";

    public static final String ECM_LIFECYCLESTATE = "ecm:currentLifeCycleState";

    public static final String ECM_VERSIONLABEL = "ecm:versionLabel";

    public static final String ECM_FULLTEXT = "ecm:fulltext";

    public static final String ECM_FULLTEXT_JOBID = "ecm:fulltextJobId";

    /** @deprecated since 5.4.2, use {@link #ECM_LOCK_OWNER} instead */
    @Deprecated
    public static final String ECM_LOCK = "ecm:lock";

    /** @since 5.4.2 */
    public static final String ECM_LOCK_OWNER = "ecm:lockOwner";

    /** @since 5.4.2 */
    public static final String ECM_LOCK_CREATED = "ecm:lockCreated";

}
