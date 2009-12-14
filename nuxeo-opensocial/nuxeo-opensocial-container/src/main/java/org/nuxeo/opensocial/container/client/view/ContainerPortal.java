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

package org.nuxeo.opensocial.container.client.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetPosition;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.gwtext.client.dd.DropTargetConfig;
import com.gwtext.client.widgets.BoxComponent;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.MessageBoxConfig;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.layout.ColumnLayoutData;
import com.gwtext.client.widgets.portal.Portal;
import com.gwtext.client.widgets.portal.PortalColumn;

/**
 * 
 * @author Guillaume Cusnieux
 */
public class ContainerPortal extends Portal {

  private static final String PORTAL_CLASS = "containerPortal";

  // note that this list is duplicated in DashboardSpaceProvider.java because
  // of linkage issues
  private static final String[] COLS = new String[] { "firstCol", "secondCol",
      "thirdCol", "fourCol" };

  private static final String MAXIMIZED_COL_ID = "maximizedCol";
  private static final String SPACER_CLASS = "x-panel-dd-spacer";
  private PortalColumn maximizedCol;

  private final Container container;

  private final Map<String, PortalColumn> columns = new HashMap<String, PortalColumn>();
  private DropTargetConfig ddConfig;
  private final Panel panel;
  private final Map<String, GadgetPortlet> portlets = new HashMap<String, GadgetPortlet>();
  private final List<GadgetPortlet> collapsedCache = new ArrayList<GadgetPortlet>();

  private int loading;
  private static GadgetPortlet maximizedPortlet;

  public ContainerPortal(Container container, Panel parent) {
    super();
    this.container = container;
    this.panel = parent;
    buildPortal();
  }

  public ContainerPortal(Container container, Panel parent,
      DropTargetConfig ddConfig) {
    this(container, parent);
    this.ddConfig = ddConfig;
  }

  @Override
  protected void afterRender() {
    new DropZone(this, ddConfig);
    loading = 0;
  }

  public Container getContainer() {
    return container;
  }

  public void buildPortal() {
    this.addClass(PORTAL_CLASS);
    this.setBorder(false);
    buildLayout();
    panel.add(this);

    List<GadgetBean> gadgets = container.getGadgets();
    List<GadgetBean> lostGadgets = new ArrayList<GadgetBean>();

    for (GadgetBean bean : gadgets)
      addGadget(bean, lostGadgets);

    for (GadgetBean bean : lostGadgets)
      addGadget(bean, null);
  }

  private GadgetPortlet addGadget(GadgetBean bean, List<GadgetBean> lostGadgets) {
    GadgetPosition pos = bean.getGadgetPosition();
    PortalColumn col = null;
    if (pos.getPlaceID() != null)
      col = columns.get(pos.getPlaceID());
    if (col == null) {
      bean.getGadgetPosition()
          .setPlaceId(COLS[0]);
      lostGadgets.add(bean);
      return null;
    } else {
      GadgetPortlet gp = new GadgetPortlet(bean);
      col.add(gp);
      portlets.put(gp.getId(), gp);
      col.doLayout();
      return gp;
    }
  }

  public static void showErrorMessage(final String title, final String message) {
    MessageBox.show(new MessageBoxConfig() {
      {
        setTitle(title);
        setMsg(message);
        setClosable(true);
        setModal(true);
      }
    });
  }

  public GadgetPosition getDropPosition() {
    for (PortalColumn col : columns.values()) {
      NodeList<Node> childs = col.getElement()
          .getChildNodes();
      for (int i = 0; i < childs.getLength(); i++) {
        Element elem = Element.as(childs.getItem(i));
        if (SPACER_CLASS.equals(elem.getClassName())) {
          return new GadgetPosition(col.getElement()
              .getId(), i);
        }
      }
    }
    return null;
  }

  public PortalColumn getPortalColumn(String id) {
    if (columns.containsKey(id))
      return columns.get(id);
    return null;
  }

  public GadgetPortlet getGadgetPortlet(String id) {
    if (portlets.containsKey(id))
      return portlets.get(id);
    return null;
  }

  public GadgetPortlet getGadgetPortletByRef(String ref) {
    String portletId;
    if (maximizedCol.isHidden()) {
      portletId = GadgetPortlet.getIdWithRefAndView(ref,
          GadgetPortlet.DEFAULT_VIEW);
      return getGadgetPortlet(portletId);
    } else
      return maximizedPortlet;
  }

  public Collection<PortalColumn> getPortalColumns() {
    return columns.values();
  }

  public void toggleGadgetsCollapse() {
    for (GadgetPortlet p : portlets.values()) {
      if (!p.isCollapsed()) {
        p.toggleCollapse();
        collapsedCache.add(p);
      }
    }
  }

  public void clearGadgetsCollapse() {
    for (GadgetPortlet p : collapsedCache) {
      p.toggleCollapse();
      collapsedCache.remove(p);
    }
  }

  private void buildLayout() {
    switch (container.getStructure()) {
    case 1:
      createLayout1Col();
      break;
    case 2:
      createLayout2Col();
      break;
    case 3:
      createLayout3Cols();
      break;
    case 4:
      createLayout4Col();
      break;
    default:
      createLayout3Cols();
      break;
    }
    createMaximizedCol();
  }

  private void createMaximizedCol() {
    maximizedCol = createCol(MAXIMIZED_COL_ID, 1.00);
    columns.remove(MAXIMIZED_COL_ID);
    maximizedCol.hide();
  }

  private void createLayout1Col() {
    createCol(COLS[0], 1.00);
  }

  private void createLayout2Col() {
    createCol(COLS[0], .5);
    createCol(COLS[1], .5);
  }

  private void createLayout3Cols() {
    createCol(COLS[0], .33);
    createCol(COLS[1], .33);
    createCol(COLS[2], .33);
  }

  private void createLayout4Col() {
    createCol(COLS[0], .25);
    createCol(COLS[1], .25);
    createCol(COLS[2], .25);
    createCol(COLS[3], .25);
  }

  private PortalColumn createCol(String id, double columnWidth) {
    PortalColumn col = new PortalColumn();
    col.setId(id);
    col.addClass(container.getLayout());
    this.add(col, new ColumnLayoutData(columnWidth));
    columns.put(id, col);
    return col;
  }

  public BoxComponent getPanel() {
    return panel;
  }

  public void removeGadgetPortlet(String id) {
    GadgetBean gadget = portlets.get(id)
        .getGadgetBean();
    portlets.remove(id);
    GadgetPosition pos = gadget.getGadgetPosition();
    PortalColumn col = columns.get(pos.getPlaceID());
    if (col == null)
      col = columns.get(COLS[0]);
    col.remove(id, true);
    col.doLayout();
  }

  public void incrementLoading() {
    if (loading < portlets.size())
      loading++;
  }

  private void replaceGadgetInDefaultPosition(GadgetPortlet portlet) {
    removeGadgetPortlet(portlet.getId());
    addGadget(portlet.getGadgetBean());
  }

  public GadgetPortlet addGadget(GadgetBean bean) {
    bean.setPosition(new GadgetPosition(COLS[0], 0));
    GadgetPortlet g = addGadget(bean, null);
    g.setVisible(true);
    columns.get(COLS[0])
        .doLayout();
    return g;
  }

  public static String getDefaultColId() {
    return COLS[0];
  }

  public static Integer getDefaultPos() {
    return 0;
  }

  public void updateColumnClassName(String oldCls, String cls,
      int oldStructure, int structure) {
    container.setLayout(cls);
    container.setStructure(structure);
    for (PortalColumn col : columns.values()) {
      col.removeClass(oldCls);
      col.addClass(cls);
      col.doLayout();
    }
    removeOldColumns(oldStructure, structure);
    addNewColumns(oldStructure, structure);
    JsLibrary.updateColumnStyle();
    this.doLayout();
    JsLibrary.updateFrameWidth();
  }

  private void addNewColumns(int oldStructure, int structure) {
    for (int i = oldStructure; i < structure; i++) {
      createCol(COLS[i], .25);
    }
    this.doLayout();
  }

  private void removeOldColumns(int oldStructure, int structure) {
    for (int i = oldStructure; i > structure; i--) {
      String colToDelete = COLS[i - 1];
      for (GadgetPortlet portlet : portlets.values()) {
        GadgetBean b = portlet.getGadgetBean();
        if (b.getGadgetPosition()
            .getPlaceID()
            .equals(colToDelete)) {
          replaceGadgetInDefaultPosition(portlet);
        }
      }
      this.columns.remove(colToDelete);
      this.remove(colToDelete, true);
    }
  }

  public PortalColumn getMaximizedCol() {
    return maximizedCol;
  }

  public static void setMaximizedPortlet(GadgetPortlet canvas) {
    maximizedPortlet = canvas;
  }

  public GadgetPortlet getGadgetPortletByFrameId(String frameId) {
    return getGadgetPortlet(GadgetPortlet.getIdWithIframeId(frameId));
  }

  public void showPortlets() {
    for (GadgetPortlet p : portlets.values()) {
      p.setVisible(true);
    }

  }

}
