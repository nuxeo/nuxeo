package org.nuxeo.dam.core.listener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dam.api.Constants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

public class InitPropertiesListener implements EventListener {

	private static final Log log = LogFactory.getLog(InitPropertiesListener.class);
	
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        
        
        // when an archive is uploaded like a .zip file, this method is called for the archive (which is an ImportSet),
        // then every folder/file in archive proceeding downwards in order
        // eg.
        // archive.zip
        //   |- PictureABC
        //   |- Folder1
        //        |- Picture1
        //        |- Picture2
        // If its an ImportSet or Folder it doesnt do anything (the second 'if' is skipped). 
        // If its a File or Picture it puts the attributes of the root ImportSet onto the current File or Picture in 
        // the archive.
        // For a single picture, its also treated as an ImportSet with a single document inside it.
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession coreSession = docCtx.getCoreSession();
            
            // Take a look at dam-schemas-contrib.xml for the ImportSet schema = dam_common + file schemas
            Map<String,Object> damMap = null;
            
            // we only need a few of the fields of the dublincore schema, so we'll create a new map with just these fields
            Map<String,Object> dublincoreMap = null;
            Map<String,Object> importSetMap = new HashMap<String,Object>();

            log.debug("");
            log.debug("Start...");
            log.debug("Document type: [" + doc.getType() + "]");
            log.debug("Document name: [" + doc.getName() + "]");

            // if the current document has the dam_common schema and is an NOT an ImportSet type (ie. is a folder or a file)
            // eg. say we are processing Picture2
            if (doc.hasSchema(Constants.DAM_COMMON_SCHEMA) && !Constants.IMPORT_SET_TYPE.equals(doc.getType())) {
            	
            	// get the parent of the current document (eg. Folder1)
                DocumentModel parent = coreSession.getDocument(doc.getParentRef());
                
                // get the root ImportSet (original archive) of the parent: eg. archive.zip
                DocumentModel importSet = docCtx.getCoreSession().getSuperSpace(parent);
                
                // get the VALUES of the dam_schema fields of the root ImportSet (archive.zip)
                damMap = importSet.getDataModel(Constants.DAM_COMMON_SCHEMA).getMap();
                
                // now set these same VALUES on the same dam_common schema for the current document (eg. Picture2)
                log.debug("Setting parent's dam_common values on current document:");
                doc.getDataModel((Constants.DAM_COMMON_SCHEMA)).setMap(damMap);
                log.debug("Done");
                
                // because the ImportSet is a composite of more than one schema, we need to get the values out of the
                // other schemas on it that we need to set on the current document. This includes the following fields and 
                // values from the dublincore schema
                dublincoreMap = importSet.getDataModel(Constants.DUBLINCORE_SCHEMA).getMap();
                
                // Iterate through these and get the fields/values that we need
                Iterator<Map.Entry<String,Object>> iterator = dublincoreMap.entrySet().iterator();
                while (iterator.hasNext()) {
                  Map.Entry<String,Object> pairs = (Map.Entry<String,Object>)iterator.next();
                  String key = (String) pairs.getKey();
                  Object value = (Object)pairs.getValue();
                  if(StringUtils.equals("dc:description", key)) {
                	// put this into our new map
                	  importSetMap.put(key, value);
                	  log.debug("Added: dc:description");
                  }
                  if(StringUtils.equals("dc:coverage", key)) {
                	// put this into our new map
                	  importSetMap.put(key, value);
                	  log.debug("Added: dc:coverage");
                  }                  
                  if(StringUtils.equals("dc:expired", key)) {
                	  // put this into our new map
                	  importSetMap.put(key, value);
                	  log.debug("Added: dc:expired");
                  }  
                  log.debug("Key: " + key);
                  log.debug("Value: " + value );
                }
                log.debug("Setting parent's dublincore values on current document:");
                // now that we have all the values that we need from the root parent ImportSet's (archive.zip's) fields
                // for the dublincore schema, set these on the current document's (eg. Picture2) dublincore schema fields
                doc.getDataModel((Constants.DUBLINCORE_SCHEMA)).setMap(importSetMap);
                log.debug("Done");
                log.debug("End...");
                log.debug("");
                
            }
        }
    }

}
