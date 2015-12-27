/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.cache;

/**
 * Helper class to check if Seam s:cache tag can be used (s:cache does not only require jboss-cache, but also some
 * internal classes.
 *
 * @author Thierry Delprat
 */
public class SeamCacheHelper {

    protected static Boolean canUseSeamCache;

    private SeamCacheHelper() {
    }

    public static boolean canUseSeamCache() {
        if (canUseSeamCache == null) {
            canUseSeamCache = false;
            try {
                Class.forName("org.jboss.system.ServiceMBeanSupport");
                canUseSeamCache = true;
            } catch (ClassNotFoundException e) {
            }
        }
        return canUseSeamCache.booleanValue();
    }
}
