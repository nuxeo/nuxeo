package org.nuxeo.opensocial.container.factory.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;

public class PermissionHelper {


  private static final Log log = LogFactory.getLog(PermissionHelper.class);

  public static final String PERMISSION_WRITE = "WRITE";


  public static boolean canWrite(String docId,CoreSession coreSession){

    try {
      DocumentModel doc = coreSession.getDocument(new IdRef(docId));
      return coreSession.hasPermission(doc.getRef(), PERMISSION_WRITE);
    } catch (ClientException e) {
      log.error(e);
      return false ;
    }

  }
}
