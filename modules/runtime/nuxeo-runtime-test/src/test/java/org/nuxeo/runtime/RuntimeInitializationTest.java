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
import static org.junit.Assert.fail;

import java.io.IOException;
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
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class RuntimeInitializationTest {

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    public RuntimeHarness harness;

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

    protected void checkErrorSource(Source source, String sourceId) {
        List<RuntimeMessage> errors = Framework.getRuntime().getMessageHandler().getRuntimeMessages(Level.ERROR);
        assertFalse(errors.isEmpty());
        errors.forEach(e -> assertEquals(source, e.getSource()));
        errors.forEach(e -> assertEquals(sourceId, e.getSourceId()));
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
        checkErrorSource(Source.COMPONENT, "invalid.comp.activate");
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-activate-message.xml")
    public void testInvalidComponentActivateMessage() {
        assertEquals(List.of("Error message on activate"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT, "invalid.comp.activate.message");
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
        checkErrorSource(Source.EXTENSION, "invalid.comp");
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-class.xml")
    public void testInvalidComponentClass() {
        assertEquals(List.of(
                "Failed to instantiate component: org.nuxeo.runtime.Foo (org.nuxeo.runtime.RuntimeServiceException: java.lang.ClassNotFoundException: org.nuxeo.runtime.Foo)"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT, "invalid.comp.class");
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-start.xml")
    public void testInvalidComponentStart() {
        assertEquals(List.of(
                "Component service:invalid.comp.start notification of application started failed: Fail on start"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT, "invalid.comp.start");
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-start-message.xml")
    public void testInvalidComponentStartMessage() {
        assertEquals(List.of("Error message on start"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT, "invalid.comp.start.message");
        assertEquals(List.of("Warn message on start"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-registration.xml")
    public void testInvalidComponentRegistration() {
        assertEquals(List.of("Failed to register extension to: service:invalid.comp.registration, xpoint: xp "
                + "in component: service:invalid.comp.registration (java.lang.RuntimeException: Fail on register)"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.EXTENSION, "invalid.comp.registration");
        assertTrue(Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING).isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:invalid-component-registration-message.xml")
    public void testInvalidComponentRegistrationMessage() {
        assertEquals(List.of("Error message on register"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR));
        checkErrorSource(Source.COMPONENT, "invalid.comp.registration.message");
        assertEquals(List.of("Warn message on register"),
                Framework.getRuntime().getMessageHandler().getMessages(Level.WARNING));
    }

    protected void checkInvalidXML(String url, String errorStartsWith, String errorEndsWith) throws Exception {
        try {
            hotDeployer.deploy(url);
            fail("IOException expected");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith(errorStartsWith));
            assertTrue(e.getMessage(), e.getMessage().endsWith(errorEndsWith));
        }
    }

    /**
     * @since 11.3
     */
    @Test
    public void testInvalidXML() throws Exception {
        // use cases for NXP-29547
        checkInvalidXML("org.nuxeo.runtime.test.tests:empty-xml.xml", "Empty registration from file:", "empty-xml.xml");
        checkInvalidXML("org.nuxeo.runtime.test.tests:invalid-xml.xml", "Could not resolve registration from file:",
                "invalid-xml.xml (org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 2; "
                        + "The markup in the document preceding the root element must be well-formed.)");
        checkInvalidXML("org.nuxeo.runtime.test.tests:log4j2-test.xml", "Could not resolve registration from file:",
                "log4j2-test.xml");
        // Non-regression test for NXP-21203
        checkInvalidXML("org.nuxeo.runtime.test.tests:invalid-xml-missing-component.xml",
                "Could not resolve registration from file:",
                "invalid-xml-missing-component.xml (Expected \"<component>\" tag for component registration, "
                        + "resolved object 'ExtensionImpl {target: service:my.comp, point:xp, contributor:null}' instead.)");
    }

    /**
     * @since 11.3
     */
    @Test
    public void testInvalidBundle() throws Exception {
        // hot deploy bundle as FeaturesRunner logics would have emptied messages if deployed with annotations
        harness.deployBundle("org.nuxeo.runtime.test.tests");

        List<RuntimeMessage> messages = Framework.getRuntime().getMessageHandler().getRuntimeMessages(Level.ERROR);
        assertEquals(4, messages.size());

        RuntimeMessage message = messages.get(0);
        assertTrue(message.getMessage().startsWith("Could not resolve registration from file:"));
        assertTrue(message.getMessage()
                          .endsWith("invalid-xml.xml (org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 2; "
                                  + "The markup in the document preceding the root element must be well-formed.)"));
        assertEquals(Level.ERROR, message.getLevel());
        assertEquals(Source.BUNDLE, message.getSource());
        assertEquals("org.nuxeo.runtime.test.tests", message.getSourceId());

        message = messages.get(1);
        assertTrue(message.getMessage().startsWith("Could not resolve registration from file:"));
        assertTrue(message.getMessage().endsWith("log4j2-test.xml"));
        assertEquals(Level.ERROR, message.getLevel());
        assertEquals(Source.BUNDLE, message.getSource());
        assertEquals("org.nuxeo.runtime.test.tests", message.getSourceId());

        message = messages.get(2);
        assertEquals("Unknown component 'invalid-file.xml' referenced by bundle 'org.nuxeo.runtime.test.tests'",
                message.getMessage());
        assertEquals(Level.ERROR, message.getLevel());
        assertEquals(Source.BUNDLE, message.getSource());
        assertEquals("org.nuxeo.runtime.test.tests", message.getSourceId());

        message = messages.get(3);
        assertTrue(message.getMessage().startsWith("Could not resolve registration from file:"));
        assertTrue(
                message.getMessage()
                       .endsWith(
                               "invalid-xml-missing-component.xml (Expected \"<component>\" tag for component registration, "
                                       + "resolved object 'ExtensionImpl {target: service:my.comp, point:xp, contributor:null}' instead.)"));
        assertEquals(Level.ERROR, message.getLevel());
        assertEquals(Source.BUNDLE, message.getSource());
        assertEquals("org.nuxeo.runtime.test.tests", message.getSourceId());
    }

}
