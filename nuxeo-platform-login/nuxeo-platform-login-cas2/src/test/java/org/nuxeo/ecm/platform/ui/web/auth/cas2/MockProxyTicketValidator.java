package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.yale.its.tp.cas.client.ProxyTicketValidator;

public class MockProxyTicketValidator extends ProxyTicketValidator {

    @Override
    public void validate() throws IOException, SAXException,
            ParserConfigurationException {

        if (callBackValidator == null) {
            return;
        }

        if (!MockProxyTicketValidator.callBackValidator.checkProxyTicketValidatorState(this)) {
            throw new RuntimeException("ProxyTicketValidator state not correct");
        }

    }

    public static CallBackCheckTicketValidatorState callBackValidator;

    public static void setCallBackValidator(
            CallBackCheckTicketValidatorState callBackValidator) {
        MockProxyTicketValidator.callBackValidator = callBackValidator;
    }

}
