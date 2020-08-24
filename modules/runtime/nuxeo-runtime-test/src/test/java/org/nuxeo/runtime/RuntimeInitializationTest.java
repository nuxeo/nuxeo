/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class RuntimeInitializationTest {

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributions() {
        // do nothing
    }

    protected void checkDupe(boolean detected) {
        List<String> errors = Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR);
        List<RuntimeMessage> errorMessages = Framework.getRuntime().getMessageHandler().getRuntimeMessages(Level.ERROR);
        if (detected) {
            assertEquals(List.of("Duplicate component name: service:my.comp2"), errors);
            assertEquals(List.of(Source.COMPONENT),
                    errorMessages.stream().map(RuntimeMessage::getSource).collect(Collectors.toList()));
            assertEquals(List.of("my.comp2"),
                    errorMessages.stream().map(RuntimeMessage::getSourceId).collect(Collectors.toList()));
        } else {
            assertTrue(errors.isEmpty());
        }
    }

    protected void checkErrorSource(Source source) {
        List<RuntimeMessage> errors = Framework.getRuntime().getMessageHandler().getRuntimeMessages(Level.ERROR);
        assertFalse(errors.isEmpty());
        errors.forEach(e -> assertEquals(source, e.getSource()));
        errors.forEach(e -> assertEquals("invalid.comp", e.getSourceId()));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    @Deploy("org.nuxeo.runtime.test.tests:CopyOfMyComp2.xml")
    public void testContributionsWithDuplicateComponent() {
        checkDupe(true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributionsWithDuplicateComponentSameFile() {
        // dupe annotation on tests not detected (?)
        checkDupe(false);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributionsWithDuplicateComponentSameFileHotReload() throws Exception {
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:MyComp2.xml");
        checkDupe(true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:MyComp1.xml")
    @Deploy("org.nuxeo.runtime.test.tests:MyComp2.xml")
    public void testContributionsWithDuplicateComponentHotReload() throws Exception {
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:CopyOfMyComp2.xml");
        checkDupe(true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-activate.xml")
    public void testInvalidComponentActivate() {
        assertEquals(List.of(
                "Failed to activate component: org.nuxeo.runtime.RuntimeInitializationTestComponent (java.lang.RuntimeException: Fail on activate)"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT);
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-activate-message.xml")
    public void testInvalidComponentActivateMessage() {
        assertEquals(List.of("Error message on activate"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT);
        assertEquals(List.of("Warn message on activate"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component.xml")
    public void testInvalidComponent() {
        assertEquals(List.of(
                "Bad extension declaration (no target attribute specified) on component 'service:invalid.comp'",
                "Warning: target extension point 'xp' of 'invalid.comp' is unknown. Check your extension in component service:invalid.comp",
                "Warning: target extension point 'null' of 'invalid.comp' is unknown. Check your extension in component service:invalid.comp"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.EXTENSION);
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-class.xml")
    public void testInvalidComponentClass() {
        assertEquals(List.of(
                "Failed to instantiate component: org.nuxeo.runtime.Foo (org.nuxeo.runtime.RuntimeServiceException: java.lang.ClassNotFoundException: org.nuxeo.runtime.Foo)"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT);
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-start.xml")
    public void testInvalidComponentStart() {
        assertEquals(
                List.of("Component service:invalid.comp notification of application started failed: Fail on start"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT);
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-start-message.xml")
    public void testInvalidComponentStartMessage() {
        assertEquals(List.of("Error message on start"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT);
        assertEquals(List.of("Warn message on start"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-registration.xml")
    public void testInvalidComponentRegistration() {
        assertEquals(List.of(
                "Failed to register extension to: service:invalid.comp, xpoint: xp in component: service:invalid.comp (java.lang.RuntimeException: Fail on register)"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.EXTENSION);
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-registration-message.xml")
    public void testInvalidComponentRegistrationMessage() {
        assertEquals(List.of("Error message on register"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT);
        assertEquals(List.of("Warn message on register"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING));
    }

}
