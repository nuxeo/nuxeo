/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.facelets.vendor;

import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletCacheFactory;

import org.nuxeo.runtime.api.Framework;

import com.sun.faces.config.WebConfiguration;

/**
 * Overrides the default JSF facelet cache factory to customize cache behaviour.
 *
 * @since 6.0
 */
public class NXFaceletCacheFactory extends FaceletCacheFactory {

    public NXFaceletCacheFactory() {
    }

    @Override
    public FaceletCache<Facelet> getFaceletCache() {
        WebConfiguration webConfig = WebConfiguration.getInstance();
        long period;
        if (Framework.isInitialized() && Framework.isDevModeSet()) {
            // force refreshPeriod to "2" when dev mode is set
            period = 2 * 1000;
        } else {
            String refreshPeriod = webConfig.getOptionValue(WebConfiguration.WebContextInitParameter.FaceletsDefaultRefreshPeriod);
            period = Long.parseLong(refreshPeriod) * 1000;
        }
        FaceletCache<Facelet> result = new DefaultFaceletCache(period);
        return result;
    }

}
