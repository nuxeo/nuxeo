/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.spaces.webobject;//

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.webengine.forms.FormData;

/**
 * Utility class for converting html forms to the 3 nuxeo-spaces concept ( i.e
 * Univers, Space and Gadget )
 * 
 * @author 10044893
 * 
 */
public final class Mapper {
  private Mapper() {
  }

  private static final class GadgetImplementation implements Gadget {

    private final String id;
    // //
    private final FormData formData;

    private GadgetImplementation(String id, FormData formData) {
      this.id = id;
      this.formData = formData;
    }

    public String getName() {
      if (id == null)
        return IdUtils.generateId(formData.getString("name"));
      else
        return formData.getString("name");
    }

    public String getDescription() {
      return formData.getString("dc:description");
    }

    public String getTitle() {

      return formData.getString("dc:title");
    }

    public Map<String, String> getPreferences() {
      return null;
    }

    public String getId() {
      if (id == null)
        return IdUtils.generateId(formData.getString("name"));
      else
        return id;
    }

    public String getCategory() {
      return null;
    }

    public String getPlaceID() {
      return null;
    }

    public int getPosition() {
      return 0;
    }

    public boolean isCollapsed() {
      return false;
    }

    public String getOwner() {
      return null;
    }

    public boolean isEqualTo(Gadget gadget) {
      return false;
    }

    public int getHeight() {
      return 0;
    }

    public String getHtmlContent() {
      return null;
    }
  }

  private static final class SpaceImplementation implements Space {
    private final FormData formData;
    private final String id;

    private SpaceImplementation(FormData formData, String id) {
      this.formData = formData;
      this.id = id;
    }

    public String getName() {
      String nameParam = formData.getString("name");
      if (id == null) {

        // on est en creation
        if (nameParam != null) {
          // on a un champ name
          return IdUtils.generateId(nameParam);
        } else {
          // on a un champ title
          return IdUtils.generateId(formData.getString("dc:title"));
        }
      } else
        return nameParam;
    }

    public String getDescription() {
      return formData.getString("dc:description");
    }

    public String getTitle() {
      return formData.getString("dc:title");
    }

    public String getId() {
      if (id == null) {
        return IdUtils.generateId(formData.getString("dc:title"));
      } else
        return id;
    }

    public String getLayout() {
      return formData.getString("layout");
    }

    public String getOwner() {
      return null;
    }

    public String getCategory() {
      return null;
    }

    public boolean isEqualTo(Space space) {
      return false;
    }

    public String getTheme() {
      return formData.getString("theme");
    }

    public Calendar getDatePublication() {

      StringTokenizer st = new StringTokenizer(formData.getString("dc:valid"),
          "/");
      Calendar date = Calendar.getInstance();
      date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(st.nextToken()));
      date.set(Calendar.MONTH, Integer.parseInt(st.nextToken()) - 1);
      date.set(Calendar.YEAR, Integer.parseInt(st.nextToken()));
      date.set(Calendar.HOUR_OF_DAY,
          Integer.parseInt(formData.getString("hours")));
      date.set(Calendar.MINUTE, Integer.parseInt(formData.getString("minutes")));
      return date;

    }

    public boolean isVersionnable() {
      return false;
    }

    public List<Space> getVersions() {
      return null;
    }

    public boolean isCurrentVersion() {
      return false;
    }

  }

  private static final class UniversImplementation implements Univers {
    private final String id;
    private final FormData formData;

    private UniversImplementation(String id, FormData formData) {
      this.id = id;
      this.formData = formData;
    }

    public String getName() {
      if (id == null)
        return IdUtils.generateId(formData.getString("name"));
      else
        return formData.getString("name");
    }

    public String getDescription() {
      return formData.getString("dc:description");
    }

    public String getTitle() {
      return formData.getString("dc:title");
    }

    public String getId() {
      if (id == null)
        return IdUtils.generateId(formData.getString("name"));
      else
        return id;
    }

    public boolean isEqualTo(Univers univers) {
      return false;
    }
  }

  public static Univers createUnivers(final FormData formData, final String id) {
    Univers univers = new UniversImplementation(id, formData);
    return univers;
  }

  public static Space createSpace(final FormData formData, final String id) {
    Space space = new SpaceImplementation(formData, id);

    return space;
  }

  public static Gadget createGadget(final FormData formData, final String id) {
    Gadget gadget = new GadgetImplementation(id, formData);

    return gadget;
  }
}
