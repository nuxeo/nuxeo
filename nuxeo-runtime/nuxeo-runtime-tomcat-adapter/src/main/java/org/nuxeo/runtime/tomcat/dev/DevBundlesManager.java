/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     slacoin
 */

package org.nuxeo.runtime.tomcat.dev;

import javax.management.MXBean;

/**
 * Expose dev bundles reloading feature to management interface
 *
 * @since 5.5
 */
@MXBean(true)
public interface DevBundlesManager {
    void loadDevBundles();

    String getDevBundlesLocation();

    void resetDevBundles(String location);

    DevBundle[] getDevBundles();

    void toggleTimer();

    boolean isTimerRunning();
}
