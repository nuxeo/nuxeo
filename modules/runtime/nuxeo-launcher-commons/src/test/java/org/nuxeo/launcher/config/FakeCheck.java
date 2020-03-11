/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.launcher.config;

import org.nuxeo.launcher.config.backingservices.BackingChecker;

public class FakeCheck implements BackingChecker {

    static private int callCount;

    private static boolean ready = true;

    @Override
    public boolean accepts(ConfigurationGenerator cg) {
        return true;
    }

    @Override
    public void check(ConfigurationGenerator cg) throws ConfigurationException {
        callCount++;
        if (!ready) {
            throw new ConfigurationException("not ready");
        }
    }

    public static void reset() {
        callCount = 0;
    }

    public static int getCallCount() {
        return callCount;
    }

    public static void setReady(boolean ready) {
        FakeCheck.ready = ready;
    }

}
