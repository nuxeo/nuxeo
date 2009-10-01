package org.nuxeo.ecm.spaces.core.dummy;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.core.contribs.api.SpaceProvider;

public class MySpaceSupplier implements SpaceProvider {

  private final class SpaceImplementation implements Space {
    public String getDescription() {
      return null;
    }

    public String getName() {
      return "myspace";
    }

    public String getTitle() {
      return "my Space";
    }

    public String getId() {
      return null;
    }

    public String getLayout() {
      return null;
    }

    public String getOwner() {
      return "myspacecreator";
    }

    public String getCategory() {
      return null;
    }

    public boolean isEqualTo(Space space) {
      // TODO Auto-generated method stub
      return false;
    }

    public String getTheme() {
      // TODO Auto-generated method stub
      return null;
    }




  }

  public boolean delete(Space element, Univers parent, CoreSession session)
      throws ClientException {
    return false;
  }

  public Space getElement(String name, Univers parent, CoreSession session)
        {
    return null;
  }

  public List<? extends Space> getElementsForParent(Univers parent,
      CoreSession session)  {
    ArrayList<Space> l = new ArrayList<Space>();
    Space mySpace = new SpaceImplementation();
    l.add(mySpace);
    return l;
  }

  public Space update(Space newOne, CoreSession session)
        {
          return newOne;

  }

  public void delete(Space element, CoreSession session)
      throws SpaceException {
    throw new SpaceException("Operation not supported");
  }

  public String getCategory() {
    return null;
  }

  public boolean canCreate() {
    // TODO Auto-generated method stub
    return false;
  }

  public Space create(Space data, Univers parent, CoreSession session)
      throws SpaceException {
    throw new SpaceException("Creation operation not supported");
  }

}
