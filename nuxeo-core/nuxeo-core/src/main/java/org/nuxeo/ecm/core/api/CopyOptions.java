/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.api.CoreSession.CopyOption;
import org.nuxeo.ecm.core.api.CoreSession.StandardCopyOption;

/**
 * @since 8.2
 */
final class CopyOptions {

    private CopyOptions() {
    }

    private boolean resetLifeCycle = false;

    private boolean resetCreator = false;

    public boolean isResetLifeCycle() {
        return resetLifeCycle;
    }

    private void setResetLifeCycle(boolean resetLifeCycle) {
        this.resetLifeCycle = resetLifeCycle;
    }

    public boolean isResetCreator() {
        return resetCreator;
    }

    private void setResetCreator(boolean resetCreator) {
        this.resetCreator = resetCreator;
    }

    static CopyOptions parse(CopyOption... options) {
        CopyOptions result = new CopyOptions();
        if (options != null) {
            for (CopyOption option : options) {
                if (option == StandardCopyOption.RESET_LIFE_CYCLE) {
                    result.setResetLifeCycle(true);
                } else if (option == StandardCopyOption.RESET_CREATOR) {
                    result.setResetCreator(true);
                } else if (option == null) {
                    throw new NullPointerException();
                } else {
                    throw new UnsupportedOperationException("'" + option + "' is not a recognized copy option");
                }
            }
        }
        return result;
    }

}
