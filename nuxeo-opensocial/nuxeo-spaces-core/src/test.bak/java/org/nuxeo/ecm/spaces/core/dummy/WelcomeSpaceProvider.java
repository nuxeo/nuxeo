package org.nuxeo.ecm.spaces.core.dummy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.core.contribs.api.SpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;

public class WelcomeSpaceProvider implements SpaceProvider {

  private static final String WELCOME_PATH = "/default-domain";
  private static Space homePageSpace = null;

  public boolean delete(Space element, Univers parent, CoreSession session)
      throws ClientException {
    return false;
  }

  public Space getElement(String name, Univers parent, CoreSession session)
      throws SpaceException {
    Space homeSpace = getWelcomePageSpace(session);
    if (homeSpace.getName()
        .equals(name))
      return homeSpace;
    return null;
  }

  public List<? extends Space> getElementsForParent(Univers parent,
      CoreSession session) throws SpaceException {
    List<Space> list = new ArrayList<Space>();
    list.add(getWelcomePageSpace(session));
    return list;
  }

  private Space getWelcomePageSpace(CoreSession session) throws SpaceException {
    PathRef pathRef = new PathRef(WELCOME_PATH);
    if (homePageSpace == null) {
      try {
        homePageSpace = createHomePageSpace(session, "accueil",
            session.getDocument(pathRef));
      } catch (ClientException e) {
        throw new SpaceException(e);
      }
    }
    return homePageSpace;
  }

  private Space createHomePageSpace(CoreSession session, String id,
      DocumentModel parent) throws SpaceException {
    DocumentModel docToCreate;
    try {
      docToCreate = session.createDocumentModel(
          parent.getPathAsString(), id, Constants.Space.TYPE);

      docToCreate.setPropertyValue("dc:title", id);
      docToCreate = session.createDocument(docToCreate);
      docToCreate.setPropertyValue("dc:title", "Page d'accueil");
      docToCreate.setPropertyValue("dc:description", "");
      docToCreate.setPropertyValue("dc:created", new Date());
      session.saveDocument(docToCreate);
      session.save();
      return docToCreate.getAdapter(Space.class);
    } catch (ClientException e) {
      throw new SpaceException(e);
    }
  }

  public Space update(Space newOne, CoreSession session)
      throws SpaceException {
    return null;
  }

  public void delete(Space element, CoreSession session)
        {

  }

  public String getCategory() {
    return "dummyWelcomeSpace";
  }



  public Space create(Space data, Univers parent, CoreSession session)
       {
    return null;
  }

}
