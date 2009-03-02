package org.nuxeo.ecm.platform.ec.notification;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ec.notification.email.EmailHelper;

public class EmailHelperMock extends EmailHelper {
    public Log log = LogFactory.getLog(this.getClass());
    public int compteur = 0;
    
    @Override
    public void sendmail(Map<String, Object> mail) throws Exception {
        compteur++;
        log.info("Faking send mail : ");
        for (String key : mail.keySet()) {
               log.info(key+" : "+mail.get(key));
        }
        
    }
    
    public int getCompteur() {
        return compteur;
    }

}
