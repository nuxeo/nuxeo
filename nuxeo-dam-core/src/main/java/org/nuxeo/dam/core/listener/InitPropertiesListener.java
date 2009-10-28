package org.nuxeo.dam.core.listener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.dam.api.Constants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class InitPropertiesListener implements EventListener {

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();

        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession coreSession = docCtx.getCoreSession();

            Map<String,Object> damMap = null;

            Map<String,Object> dublincoreMap = null;
            Map<String,Object> importSetMap = new HashMap<String,Object>();

            if (doc.hasSchema(Constants.DAM_COMMON_SCHEMA) && !Constants.IMPORT_SET_TYPE.equals(doc.getType())) {

                DocumentModel parent = coreSession.getDocument(doc.getParentRef());
                DocumentModel importSet = docCtx.getCoreSession().getSuperSpace(parent);

                damMap = importSet.getDataModel(Constants.DAM_COMMON_SCHEMA).getMap();
                doc.getDataModel((Constants.DAM_COMMON_SCHEMA)).setMap(damMap);

                dublincoreMap = importSet.getDataModel(Constants.DUBLINCORE_SCHEMA).getMap();

                Iterator<Map.Entry<String,Object>> iterator = dublincoreMap.entrySet().iterator();
                while (iterator.hasNext()) {
                  Map.Entry<String,Object> pairs = (Map.Entry<String,Object>)iterator.next();
                  String key =  pairs.getKey();
                  Object value = pairs.getValue();
                  if("dc:description".equals(key)) {
                	  importSetMap.put(key, value);
                  } else  if("dc:coverage".equals(key)) {
                	  importSetMap.put(key, value);
                  } else if("dc:expired".equals(key)) {
                	  importSetMap.put(key, value);
                  }

                }
                doc.getDataModel((Constants.DUBLINCORE_SCHEMA)).setMap(importSetMap);
            }
        }
    }

}
