package org.nuxeo.ecm.spaces.core.contribs.impl;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.core.contribs.api.SpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.AbstractProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.spaces.core.impl.DocumentHelper;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocumentWrapper;

/**
 *
 * @author 10044893
 *
 */
public class DefaultSpaceProvider extends
    AbstractProvider<Space, Univers> implements SpaceProvider{

  public DefaultSpaceProvider(){
    super(Space.class,Constants.Space.TYPE, "defaultSpace");
  }


  public Space create(Space data,
      Univers parent, CoreSession session)
      throws SpaceException {

    // common creation of document
    DocumentModel doc;
    try {
      doc = DocumentHelper.createInternalDocument(
          ((DocumentWrapper)parent).getInternalDocument(), data.getName(), data.getTitle(),
          data.getDescription(), session,Constants.Space.TYPE);
      // complete with specific space properties
      doc.setPropertyValue(Constants.Space.SPACE_THEME, data.getTheme());
      doc.setPropertyValue(Constants.Space.SPACE_LAYOUT, data.getLayout());
        doc.setPropertyValue(Constants.Space.SPACE_CATEGORY, data.getCategory());

      // save document and session
      session.saveDocument(doc);
      session.save();

    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }



    return getAdaptedDocument(doc);
  }

  public Space update(Space data, CoreSession session)
      throws SpaceException {

    DocumentModel documentModel;
    try {
      documentModel = DocumentHelper.getDocumentById(data.getId(), session);

    // common update
     documentModel = DocumentHelper.updateDocument(documentModel, session,data.getName(),data.getTitle(),data.getDescription());

    // specific update for a space document
     documentModel.setPropertyValue(Constants.Space.SPACE_THEME, data.getTheme());
    documentModel.setPropertyValue(Constants.Space.SPACE_LAYOUT, data.getLayout());
    documentModel.setPropertyValue(Constants.Space.SPACE_CATEGORY, data.getCategory());

    // save document and session
    session.saveDocument(documentModel);
    session.save();

    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }


    return documentModel.getAdapter(Space.class);
  }

}
