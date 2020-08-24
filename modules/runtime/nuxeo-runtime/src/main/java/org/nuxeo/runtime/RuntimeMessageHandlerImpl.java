/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.model.ComponentManager;

/**
 * Handles runtime messages by taking care of component manager lifecycle in order to work correctly with hot reload.
 * This is interesting to not store several time the same message in case of hot reload.
 *
 * @since 9.10
 */
public class RuntimeMessageHandlerImpl implements RuntimeMessageHandler, ComponentManager.Listener {

    protected ComponentManagerStep step;

    protected final List<RuntimeMessage> messages = new ArrayList<>();

    @Override
    @Deprecated
    public void addWarning(String message) {
        addMessage(Level.WARNING, message);
    }

    @Override
    @Deprecated
    public List<String> getWarnings() {
        return getMessages(Level.WARNING);
    }

    @Override
    @Deprecated
    public void addError(String message) {
        addMessage(Level.ERROR, message);
    }

    @Override
    @Deprecated
    public List<String> getErrors() {
        return getMessages(Level.ERROR);
    }

    @Override
    public void beforeActivation(ComponentManager mgr) {
        changeStep(ComponentManagerStep.ACTIVATING);
    }

    @Override
    public void beforeStart(ComponentManager mgr, boolean isResume) {
        changeStep(ComponentManagerStep.STARTING);
    }

    @Override
    public void afterStart(ComponentManager mgr, boolean isResume) {
        changeStep(ComponentManagerStep.RUNNING);
    }

    @Override
    public void beforeStop(ComponentManager mgr, boolean isStandby) {
        changeStep(ComponentManagerStep.STOPPING);
    }

    @Override
    public void beforeDeactivation(ComponentManager mgr) {
        changeStep(ComponentManagerStep.DEACTIVATING);
    }

    protected void changeStep(ComponentManagerStep step) {
        if (this.step == ComponentManagerStep.RUNNING) {
            // reset bundle/component/extension messages when previous step was "running"
            messages.removeIf(m -> m.getSource() != null
                    && Set.of(Source.BUNDLE, Source.COMPONENT, Source.EXTENSION).contains(m.getSource()));
        }
        this.step = step;
    }

    @Override
    public void addMessage(RuntimeMessage message) {
        messages.add(message);
    }

    @Override
    public void addMessage(Level level, String message) {
        addMessage(new RuntimeMessage(level, message));
    }

    @Override
    public List<String> getMessages(Level level) {
        return getMessages(msg -> level.equals(msg.getLevel()));
    }

    @Override
    public List<String> getMessages(Predicate<RuntimeMessage> predicate) {
        return messages.stream()
                       .filter(predicate)
                       .map(RuntimeMessage::getMessage)
                       .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<RuntimeMessage> getRuntimeMessages(Level level) {
        return getRuntimeMessages(msg -> level.equals(msg.getLevel()));
    }

    @Override
    public List<RuntimeMessage> getRuntimeMessages(Predicate<RuntimeMessage> predicate) {
        return messages.stream().filter(predicate).collect(Collectors.toUnmodifiableList());
    }

    protected enum ComponentManagerStep {

        ACTIVATING,

        STARTING,

        RUNNING,

        STOPPING,

        DEACTIVATING

    }

}
