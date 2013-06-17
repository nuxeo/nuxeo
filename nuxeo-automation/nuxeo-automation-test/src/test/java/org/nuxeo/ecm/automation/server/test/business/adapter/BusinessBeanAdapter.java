package org.nuxeo.ecm.automation.server.test.business.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Document Model Adapter example server side
 */
public class BusinessBeanAdapter extends BusinessAdapter {

    private static final Log log = LogFactory.getLog(BusinessBeanAdapter.class);

    public BusinessBeanAdapter() {
        super();
    }

    public BusinessBeanAdapter(DocumentModel documentModel) {
        super(documentModel);
    }

    public String getTitle() {
        try {
            return (String) doc.getPropertyValue("dc:title");
        } catch (ClientException e) {
            log.error("cannot get property title", e);
        }
        return null;
    }

    public void setTitle(String value) {
        try {
            doc.setPropertyValue("dc:title", value);
        } catch (ClientException e) {
            log.error("cannot set property title", e);
        }
    }

    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (ClientException e) {
            log.error("cannot get description property", e);
        }
        return null;
    }

    public void setDescription(String value) {
        try {
            doc.setPropertyValue("dc:description", value);
        } catch (ClientException e) {
            log.error("cannot set description property", e);
        }
    }

}
