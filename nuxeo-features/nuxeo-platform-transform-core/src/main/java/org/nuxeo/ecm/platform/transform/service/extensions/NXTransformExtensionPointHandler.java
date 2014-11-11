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
 * $Id: NXTransformExtensionPointHandler.java 20642 2007-06-17 13:14:51Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform.service.extensions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.service.TransformService;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class defining helpers for TransformServiceCommon extensions.
 *
 * @author janguenot
 */
public abstract class NXTransformExtensionPointHandler {

    protected static final Log log = LogFactory.getLog(NXTransformExtensionPointHandler.class);

    /**
     * Gets the NXTransformBean EJB remotely performing a JNDI lookup.
     *
     * @return TransformServiceCommon bean
     */
    protected static TransformServiceCommon getNXTransform() {
        return (TransformServiceCommon) Framework.getRuntime().getComponent(TransformService.NAME);
    }

}
