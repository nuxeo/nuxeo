/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.work;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadEventNames;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class ReloadListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        if (ReloadEventNames.BEFORE_RELOAD_EVENT_ID.equals(event.getId())) {
            try {
                if (!Framework.getService(WorkManager.class).shutdown(10, TimeUnit.SECONDS)) {
                    LogFactory.getLog(ReloadListener.class).warn("timed out while shutdowning works");
                } else {
                    LogFactory.getLog(ReloadListener.class).debug("works shutdown");
                }
            } catch (InterruptedException cause) {
                Thread.currentThread().interrupt();
                LogFactory.getLog(ReloadListener.class).error("Caugh interruption while stopping works manager", cause);
            }
        } else if (ReloadEventNames.AFTER_RELOAD_EVENT_ID.equals(event.getId())) {
            Framework.getService(WorkManager.class).init();
        }
    }

}
