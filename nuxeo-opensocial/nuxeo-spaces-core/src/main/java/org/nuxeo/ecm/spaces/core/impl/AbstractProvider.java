package org.nuxeo.ecm.spaces.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocumentWrapper;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;

public abstract class AbstractProvider<E, P>
      {

  private Class<E> factoryClass = null;
  private String type = null;
  private String category;

  @SuppressWarnings("unused")
  private AbstractProvider() {
  }

  public AbstractProvider(Class<E> factoryClass, String aType, String category) {
    this.factoryClass = factoryClass;
    this.type = aType;
    this.category = category;
  }

  public String getCategory() {
    return category;
  }

  public void delete(E element, CoreSession session) throws SpaceException {
    try {
      DocumentHelper.delete(((DocumentWrapper) element).getInternalDocument(), session);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      throw new SpaceException(e);
    }
  }



  public E getElement(String name, P parent, CoreSession session)
      throws NoElementFoundException, SpaceSecurityException {
    DocumentModel spacesRoot = ((DocumentWrapper) parent).getInternalDocument();

    DocumentModel doc;
    try {
      doc = session.getChild(spacesRoot.getRef(), name);
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      //TODO DDSL voir pour affiner
      throw new NoElementFoundException(e);
    }
    if (doc.getType() != null && type != null && doc.getType()
        .equals(type))
      return getAdaptedDocument(doc);
    throw new NoElementFoundException();
  }

  public List<? extends E> getElementsForParent(P parent, CoreSession session)
      throws SpaceException {
    List<E> retour = new ArrayList<E>();

    DocumentModel parentDocument = ((DocumentWrapper) parent).getInternalDocument();
    DocumentModelList docs;
    try {
      docs = session.getChildren(parentDocument.getRef());
    } catch (DocumentSecurityException e) {
      throw new SpaceSecurityException(e);
    }catch (ClientException e) {
      //TODO DDSL voir pour affiner
      throw new SpaceException(e);
    }

    for (DocumentModel doc : docs) {

      if (doc.getType() != null && type != null && doc.getType()
          .equals(type))
        retour.add(getAdaptedDocument(doc));
    }
    return retour;
  }

  protected E getAdaptedDocument(DocumentModel doc) {
    return doc.getAdapter(factoryClass);
  }



}
