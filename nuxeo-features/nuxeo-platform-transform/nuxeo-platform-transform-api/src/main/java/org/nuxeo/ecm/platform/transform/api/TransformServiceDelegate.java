/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TransformServiceDelegate.java 20875 2007-06-19 20:26:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.api;

import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.runtime.api.Framework;

/**
 * Stateless transform service delegate.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * @deprecated TransformationService is deprecated,
 *    please use {@see org.nuxeo.ecm.core.convert.api.ConversionService}
 */
@Deprecated
public final class TransformServiceDelegate {

    // Utility class.
    private TransformServiceDelegate() {
    }

    public static TransformServiceCommon getRemoteTransformService()
            throws TransformException {
        TransformServiceCommon service;
        try {
            service = Framework.getService(TransformServiceCommon.class);
        } catch (Exception e) {
            throw new TransformException(e);
        }
        return service;
    }

    public static TransformServiceCommon getLocalTransformService()
            throws TransformException {
        TransformServiceCommon service;
        try {
            service = Framework.getLocalService(TransformServiceCommon.class);
        } catch (Exception e) {
            throw new TransformException(e);
        }
        return service;
    }

}
