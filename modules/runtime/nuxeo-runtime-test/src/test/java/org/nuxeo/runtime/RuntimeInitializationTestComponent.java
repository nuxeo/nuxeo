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

import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.3
 */
public class RuntimeInitializationTestComponent extends DefaultComponent {

    protected String actionType;

    protected String actionTypeOn;

    @Override
    public void activate(ComponentContext context) {
        actionType = (String) context.getPropertyValue("actionType");
        actionTypeOn = (String) context.getPropertyValue("actionTypeOn");

        actOn("activate");
    }

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        actOn("register");
    }

    @Override
    public void start(ComponentContext context) {
        actOn("start");
    }

    protected void actOn(String action) {
        if (!action.equals(actionTypeOn)) {
            return;
        }
        if ("fail".equals(actionType)) {
            throw new RuntimeException("Fail on " + actionTypeOn);
        }
        if ("message".equals(actionType)) {
            addRuntimeMessage(Level.ERROR, "Error message on " + actionTypeOn);
            addRuntimeMessage(Level.WARNING, "Warn message on " + actionTypeOn);
        }
    }

}
