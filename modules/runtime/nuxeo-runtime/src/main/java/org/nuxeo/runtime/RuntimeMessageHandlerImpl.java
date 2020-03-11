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
 */
package org.nuxeo.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.nuxeo.runtime.model.ComponentManager;

/**
 * Handles runtime messages by taking care of component manager lifecycle in order to work correctly with hot reload.
 * This is interesting to not store several time the same message in case of hot reload.
 *
 * @since 9.10
 */
public class RuntimeMessageHandlerImpl implements RuntimeMessageHandler, ComponentManager.Listener {

    protected final List<Message> messages = new ArrayList<>();

    protected ComponentManagerStep step = ComponentManagerStep.ACTIVATING;

    @Override
    public void addWarning(String message) {
        messages.add(new Message(step, Level.WARNING, message));
    }

    @Override
    public List<String> getWarnings() {
        return messages.stream().filter(msg -> Level.WARNING.equals(msg.getLevel())).map(Message::getMessage).collect(
                Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    @Override
    public void addError(String message) {
        messages.add(new Message(step, Level.SEVERE, message));
    }

    @Override
    public List<String> getErrors() {
        return messages.stream().filter(msg -> Level.SEVERE.equals(msg.getLevel())).map(Message::getMessage).collect(
                Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
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
        messages.removeIf(msg -> step.equals(msg.getStep()));
        this.step = step;
    }

    /**
     * @since 9.10
     */
    protected static class Message {

        protected final ComponentManagerStep step;

        protected final Level level;

        protected final String message;

        public Message(ComponentManagerStep step, Level level, String message) {
            this.step = step;
            this.level = level;
            this.message = message;
        }

        public ComponentManagerStep getStep() {
            return step;
        }

        public Level getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

    }

    /**
     * @since 9.10
     */
    protected enum ComponentManagerStep {

        ACTIVATING,

        STARTING,

        RUNNING,

        STOPPING,

        DEACTIVATING

    }

}
