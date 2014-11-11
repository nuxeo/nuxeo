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
 *
 * $Id: WMProcessDefinition.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;

/**
 * Process definition.
 * <p>
 * Note : getVersion() not in wfmc spec.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMProcessDefinition extends Serializable {

    /**
     * Returns the process definition id.
     *
     * @return the process definition id
     */
    String getId();

    /**
     * Returns the process definition name.
     *
     * @return the process definition name
     */
    String getName();

    /**
     * Returns the process definition version.
     *
     * @return the process definition version
     */
    int getVersion();

}
