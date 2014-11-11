/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 *     Stephane Lacoin at Nuxeo (aka matic) <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.platform.web.common.locale;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.6
 */
@XObject("provider")
public class LocaleProviderDescriptor {

    @XNode("@class")
    protected Class<LocaleProvider> clazz;

    public LocaleProvider newProvider() {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create locale provider", e);
        }
    }
}
