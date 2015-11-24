package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.yale.its.tp.cas.client.ServiceTicketValidator;

public class MockServiceTicketValidator extends ServiceTicketValidator {

    @Override
    public void validate() throws IOException, SAXException,
            ParserConfigurationException {

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

    public static void setCallBackValidator(
            CallBackCheckTicketValidatorState callBackValidator) {
        MockProxyTicketValidator.callBackValidator = callBackValidator;
    }

}
