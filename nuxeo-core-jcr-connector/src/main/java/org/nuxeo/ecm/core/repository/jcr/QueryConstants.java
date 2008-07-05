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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface QueryConstants {

    public final static String ECM_TYPE = "ecm:type";
    public final static String ECM_PATH = "ecm:path";
    public final static String ECM_SCHEMA = "ecm:schema";
    public final static String ECM_ID = "ecm:id";
    public final static String ECM_FULLTEXT = "ecm:fulltext";
    public final static String ECM_NAME = "ecm:name";
    public final static String ECM_FROZEN_NODE = "ecm:frozenNode";
    public final static String ECM_VERSION = "ecm:version";
    /**
     *  only for compatibility
     *  @deprecated use {@link #ECM_VERSION}
     */
    public final static String ECM_IS_CHECKED_IN_VERSION = "ecm:isCheckedInVersion";

}
