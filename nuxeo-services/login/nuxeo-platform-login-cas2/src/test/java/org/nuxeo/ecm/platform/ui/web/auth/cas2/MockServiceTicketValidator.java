/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.yale.its.tp.cas.client.ServiceTicketValidator;

public class MockServiceTicketValidator extends ServiceTicketValidator {

    @Override
    public void validate() throws IOException, SAXException, ParserConfigurationException {

        if (callBackValidator != null && !MockProxyTicketValidator.callBackValidator.checkTicketValidatorState(this)) {
            errorMessage = "Error during ticket validation";
            errorCode = "10";
            successfulAuthentication = false;
        }

        user = st;
        pgtIou = st + "PgtIou";
        successfulAuthentication = true;
    }

    public static CallBackCheckTicketValidatorState callBackValidator;

    public static void setCallBackValidator(CallBackCheckTicketValidatorState callBackValidator) {
        MockProxyTicketValidator.callBackValidator = callBackValidator;
    }

}
