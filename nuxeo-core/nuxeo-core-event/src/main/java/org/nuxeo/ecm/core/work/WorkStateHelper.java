/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.work;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * Work state helper to handle, out-of-API, distributed, work states.<br>
 *
 * @since 10.2
 */
public class WorkStateHelper {

    protected static final String KV_NAME = "workManager";

    protected static final String STATE_SUFFIX = ":state";

    protected static final String CANCELED = "canceled";

    protected static KeyValueStore getKeyValueStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(KV_NAME);
    }

    protected static Work.State getState(String workId) {
        String stringState = getKeyValueStore().getString(getStateKey(workId));
        return stringState == null || CANCELED.equals(stringState) ? null : Work.State.valueOf(stringState);
    }

    protected static String getStateKey(String workId) {
        return workId + STATE_SUFFIX;
    }

    protected static boolean isCanceled(String workId) {
        return CANCELED.equals(getKeyValueStore().getString(getStateKey(workId)));
    }

    protected static void setCanceled(String workId) {
        setState(workId, CANCELED, 0);
    }

    protected static void setState(String workId, String state, long ttl) {
        getKeyValueStore().put(getStateKey(workId), state, ttl);
    }

    private WorkStateHelper() {
        // hide constructor
    }

}
