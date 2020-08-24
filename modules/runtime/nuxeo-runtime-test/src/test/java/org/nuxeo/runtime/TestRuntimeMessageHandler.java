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

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test runtime message handler api.
 *
 * @see RuntimeInitializationTest for default usage tests.
 * @since 11.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestRuntimeMessageHandler {

    protected static final String PATTERN = "%s (Level '%s')";

    protected static RuntimeMessageHandler getMessageHandler() {
        return Framework.getRuntime().getMessageHandler();
    }

    protected static void addTestMessages(String prefix) {
        getMessageHandler().addMessage(Level.ERROR, String.format(PATTERN, prefix, Level.ERROR));
        getMessageHandler().addMessage(Level.WARNING, String.format(PATTERN, prefix, Level.WARNING));
    }

    protected void checkHasMessage(boolean hasMessage, Level level, String msg) {
        assertEquals(hasMessage, getMessageHandler().getMessages(level).contains(msg));
    }

    protected void checkHasMessages(boolean hasMessage, String prefix) {
        checkHasMessage(hasMessage, Level.ERROR, String.format(PATTERN, prefix, Level.ERROR));
        checkHasMessage(hasMessage, Level.WARNING, String.format(PATTERN, prefix, Level.WARNING));
    }

    @BeforeClass
    public static void addMessageBeforeClass() {
        addTestMessages("Before class message");
    }

    @Test
    public void testAddMessageBefore() {
        assertEquals(2, getMessageHandler().getMessages(m -> true).size());
        checkHasMessages(true, "Before class message");
    }

    @Test
    public void testAddMessage() {
        checkHasMessages(false, "foo");
        assertEquals(2, getMessageHandler().getMessages(m -> true).size());

        addTestMessages("Test add message");

        checkHasMessages(false, "foo");
        checkHasMessages(true, "Test add message");
        assertEquals(4, getMessageHandler().getMessages(m -> true).size());

        getMessageHandler().addMessage(
                new RuntimeMessage(Level.ERROR, "test message with source", Source.CODE, "testId"));

        checkHasMessage(true, Level.ERROR, "test message with source");
        assertEquals(5, getMessageHandler().getMessages(m -> true).size());
        assertEquals(3, getMessageHandler().getMessages(Level.ERROR).size());
        assertEquals(3, getMessageHandler().getRuntimeMessages(Level.ERROR).size());
        assertEquals(2, getMessageHandler().getMessages(Level.WARNING).size());
        assertEquals(2, getMessageHandler().getRuntimeMessages(Level.WARNING).size());

        assertEquals(1, getMessageHandler().getRuntimeMessages(m -> Source.CODE.equals(m.getSource())).size());
        assertEquals(1, getMessageHandler().getRuntimeMessages(m -> "testId".equals(m.getSourceId())).size());
    }

}
