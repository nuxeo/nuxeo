/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: LifeCycleConstants.java 19491 2007-05-27 13:51:18Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

/**
 * Life cycle constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class LifeCycleConstants {

    public static final String LIFECYCLE_SCHEMA_URI = "http://www.nuxeo.org/ecm/schemas/lifecycle";

    public static final String LIFECYCLE_SCHEMA_PREFIX = "lc";

    public static final String LIFECYCLE_SCHEMA_NAME = "lifecycle";

    // JCR2 names.
    public static final String LIFECYCLE_POLICY_PROP = "lifecyclePolicy";

    public static final String LIFECYCLE_STATE_PROP = "currentLifecycleState";

    public static final String INITIAL_LIFECYCLE_STATE_OPTION_NAME = "initialLifecycleState";

    // Constant utility class
    private LifeCycleConstants() {
    }

}
