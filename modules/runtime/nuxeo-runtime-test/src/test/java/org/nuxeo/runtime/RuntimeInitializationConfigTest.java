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
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Similar tests to {@link RuntimeInitializationTest}, pushing resources to the runtime test config directory,
 * "auto-discovered" by the framework.
 *
 * @since 11.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, RuntimeInitializationConfigErrorFeature.class })
public class RuntimeInitializationConfigTest {

    protected void checkMessage(RuntimeMessage runtimeMessage, String message, Level level, Source source,
            String sourceId) {
        assertEquals(message, runtimeMessage.getMessage());
        assertEquals(level, runtimeMessage.getLevel());
        assertEquals(source, runtimeMessage.getSource());
        assertEquals(sourceId, runtimeMessage.getSourceId());
    }

    protected void checkMessage(RuntimeMessage runtimeMessage, String messageStartsWith, String messageEndsWith,
            Level level, Source source, String sourceId) {
        assertTrue(runtimeMessage.getMessage(), runtimeMessage.getMessage().startsWith(messageStartsWith));
        assertTrue(runtimeMessage.getMessage(), runtimeMessage.getMessage().endsWith(messageEndsWith));
        assertEquals(level, runtimeMessage.getLevel());
        assertEquals(source, runtimeMessage.getSource());
        assertEquals(sourceId, runtimeMessage.getSourceId());
    }

    @Test
    public void testLoadConfig() throws Exception {
        // errors
        List<RuntimeMessage> errors = Framework.getRuntime().getMessageHandler().getRuntimeMessages(Level.ERROR);
        assertEquals(15, errors.size());

        Iterator<RuntimeMessage> errorsIt = errors.iterator();
        checkMessage(errorsIt.next(), "Error deploying config empty-xml-config.xml (Empty registration from file:",
                "config/empty-xml-config.xml)", Level.ERROR, Source.CONFIG, "empty-xml-config.xml");
        checkMessage(errorsIt.next(), "Duplicate component name: service:invalid.comp.start.message", Level.ERROR,
                Source.COMPONENT, "invalid.comp.start.message");
        checkMessage(errorsIt.next(),
                "Error deploying config invalid-xml-config.xml (Could not resolve registration from file:",
                "config/invalid-xml-config.xml (org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 2; "
                        + "The markup in the document preceding the root element must be well-formed.))",
                Level.ERROR, Source.CONFIG, "invalid-xml-config.xml");
        checkMessage(errorsIt.next(),
                "Error deploying config invalid-xml-missing-component-config.xml (Could not resolve registration from file:",
                "config/invalid-xml-missing-component-config.xml (Expected \"<component>\" tag for component registration, "
                        + "resolved object 'ExtensionImpl {target: service:my.comp, point:xp, contributor:null}' instead.))",
                Level.ERROR, Source.CONFIG, "invalid-xml-missing-component-config.xml");
        checkMessage(errorsIt.next(),
                "Error deploying config log4j2-test-config.xml (Could not resolve registration from file:",
                "config/log4j2-test-config.xml)", Level.ERROR, Source.CONFIG, "log4j2-test-config.xml");
        checkMessage(errorsIt.next(),
                "Failed to instantiate component: org.nuxeo.runtime.Foo (org.nuxeo.runtime.RuntimeServiceException: "
                        + "java.lang.ClassNotFoundException: org.nuxeo.runtime.Foo)",
                Level.ERROR, Source.COMPONENT, "invalid.comp.class");
        checkMessage(errorsIt.next(),
                "Failed to activate component: org.nuxeo.runtime.RuntimeInitializationTestComponent (java.lang.RuntimeException: Fail on activate)",
                Level.ERROR, Source.COMPONENT, "invalid.comp.activate");
        checkMessage(errorsIt.next(), "Error message on activate", Level.ERROR, Source.COMPONENT,
                "invalid.comp.activate.message");
        checkMessage(errorsIt.next(),
                "Bad extension declaration (no target attribute specified) on component 'service:invalid.comp'",
                Level.ERROR, Source.EXTENSION, "invalid.comp");
        checkMessage(errorsIt.next(),
                "Warning: target extension point 'xp' of 'invalid.comp' is unknown. Check your extension in component service:invalid.comp",
                Level.ERROR, Source.EXTENSION, "invalid.comp");
        checkMessage(errorsIt.next(),
                "Warning: target extension point 'null' of 'invalid.comp' is unknown. Check your extension in component service:invalid.comp",
                Level.ERROR, Source.EXTENSION, "invalid.comp");
        checkMessage(errorsIt.next(), "Failed to register extension to: service:invalid.comp.registration, xpoint: xp "
                + "in component: service:invalid.comp.registration (java.lang.RuntimeException: Fail on register)",
                Level.ERROR, Source.EXTENSION, "invalid.comp.registration");
        checkMessage(errorsIt.next(), "Error message on register", Level.ERROR, Source.COMPONENT,
                "invalid.comp.registration.message");
        checkMessage(errorsIt.next(),
                "Component service:invalid.comp.start notification of application started failed: Fail on start",
                Level.ERROR, Source.COMPONENT, "invalid.comp.start");
        checkMessage(errorsIt.next(), "Error message on start", Level.ERROR, Source.COMPONENT,
                "invalid.comp.start.message");

        // warnings
        List<RuntimeMessage> warnings = Framework.getRuntime().getMessageHandler().getRuntimeMessages(Level.WARNING);
        assertEquals(3, warnings.size());

        Iterator<RuntimeMessage> warningsIt = warnings.iterator();
        checkMessage(warningsIt.next(), "Warn message on activate", Level.WARNING, Source.COMPONENT,
                "invalid.comp.activate.message");
        checkMessage(warningsIt.next(), "Warn message on register", Level.WARNING, Source.COMPONENT,
                "invalid.comp.registration.message");
        checkMessage(warningsIt.next(), "Warn message on start", Level.WARNING, Source.COMPONENT,
                "invalid.comp.start.message");
    }

}
