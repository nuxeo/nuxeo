/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.jsf.facelets.vendor;

import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletCacheFactory;

import com.sun.faces.config.WebConfiguration;

/**
 * Overrides the default JSF facelet cache factory to customize cache
 * behaviour.
 *
 * @since 5.9.4-JSF2
 */
public class NXFaceletCacheFactory extends FaceletCacheFactory {

    public NXFaceletCacheFactory() {
    }

    @Override
    public FaceletCache getFaceletCache() {
        WebConfiguration webConfig = WebConfiguration.getInstance();
        String refreshPeriod = webConfig.getOptionValue(WebConfiguration.WebContextInitParameter.FaceletsDefaultRefreshPeriod);
        long period = Long.parseLong(refreshPeriod) * 1000;
        FaceletCache<Facelet> result = new DefaultFaceletCache(period);
        return result;
    }

}
