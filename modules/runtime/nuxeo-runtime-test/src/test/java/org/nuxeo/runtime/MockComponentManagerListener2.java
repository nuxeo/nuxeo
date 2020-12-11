/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime;

import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.5
 */
public class MockComponentManagerListener2 extends DefaultComponent implements ComponentManager.Listener {

    public static final String NAME = "component.manager.listener2";

    public MockEventsInfo info = new MockEventsInfo();

    @Override
    public void beforeStop(ComponentManager mgr, boolean isStandby) {
        info.beforeStop++;
    }

    @Override
    public void beforeStart(ComponentManager mgr, boolean isResume) {
        info.beforeStart++;
    }

    @Override
    public void afterStop(ComponentManager mgr, boolean isStandby) {
        info.afterStop++;
    }

    @Override
    public void afterStart(ComponentManager mgr, boolean isResume) {
        info.afterStart++;
    }

}
