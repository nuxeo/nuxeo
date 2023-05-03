/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.opensaml.common.SAMLObject;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @since 2023.0
 */
public record ExpectedSAMLMessage<O extends SAMLObject> (String message, Function<O, Object>... paramGetters) {

    @SafeVarargs
    public ExpectedSAMLMessage {
    }

    @SuppressWarnings("unchecked")
    public String unmarshallThenFormatExpected(String actualSamlMessage) {
        try {
            var samlObject = unmarshallSAMLMessage(actualSamlMessage);
            var args = Stream.of(paramGetters).map(f -> f.apply((O) samlObject)).toArray(Object[]::new);
            return message.formatted(args);
        } catch (ClassCastException e) {
            throw new AssertionError("The actual SAML message is not of the expected type, raw:\n" + actualSamlMessage,
                    e);
        }
    }

    protected SAMLObject unmarshallSAMLMessage(String message) {
        try (var is = IOUtils.toInputStream(message, UTF_8)) {
            Document messageDoc = new BasicParserPool().parse(is);
            Element messageElem = messageDoc.getDocumentElement();

            Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(messageElem);

            return (SAMLObject) unmarshaller.unmarshall(messageElem);
        } catch (IOException | XMLParserException | UnmarshallingException e) {
            throw new AssertionError("Unable to unmarshall the message", e);
        }
    }
}
