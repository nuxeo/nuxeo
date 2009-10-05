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

import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.JsLibrary;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetPosition;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.ExtElement;
import com.gwtext.client.dd.DragData;
import com.gwtext.client.dd.DragSource;
import com.gwtext.client.dd.DropTarget;
import com.gwtext.client.dd.DropTargetConfig;
import com.gwtext.client.dd.ScrollManager;
import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.Container;
import com.gwtext.client.widgets.portal.PanelProxy;
import com.gwtext.client.widgets.portal.PortalColumn;

/**
 * DropZone serve for catch drag and drop event and call container service for
 * save
 *
 * @author 10044826
 *
 */
public class DropZone extends DropTarget {

  private static final String DEFAULT = "default";
  private ContainerPortal portal;
  private int lastCW = -1;
  // private int lastPos = -1;
  private PosGrid[] grid;

  private Container lastPosC;
  private int col;
  private int[] scrollPos;

  public DropZone(ContainerPortal portal, DropTargetConfig config) {
    super(portal.getBodyWrap()
        .getDOM(), config);
    ScrollManager.register(portal.getBody()
        .getDOM());
    this.portal = portal;
  }

  @Override
  public String notifyOver(DragSource source, EventObject e, DragData data) {
    int[] xy = e.getXY();
    PanelProxy proxy = new PanelProxy(source.getProxy()
        .getJsObj());

    if (grid == null)
      grid = getGrid();

    int cw = portal.getBody()
        .getClientWidth();
    if (lastCW == -1) {
      lastCW = cw;
    } else if (lastCW != cw) {
      lastCW = cw;
      portal.doLayout();
      grid = getGrid();
    }
    // determine column
    col = 0;
    boolean cmatch = false;
    for (int len = grid.length; col < len; col++) {
      PosGrid posGrid = grid[col];
      if (posGrid.isCol(xy)) {
        cmatch = true;
        break;
      }
    }

    // no match, fix last index
    if (!cmatch)
      col--;

    // find insert position
    boolean match = false;
    int pos = 0;
    lastPosC = (Container) portal.getItems()[col];
    Component[] items = lastPosC.getItems();
    Component p = null;

    for (pos = 0; pos < items.length; pos++) {
      p = items[pos];
      int height = p.getEl()
          .getHeight();
      if (height != 0 && (p.getEl()
          .getY() + (height / 2)) > xy[1]) {
        match = true;
        break;
      }
    }

    proxy.getProxy()
        .setWidth("auto", false);

    if (p != null) {
      proxy.moveProxy(p.getEl()
          .getParentNode(), (match ? p.getEl()
          .getDOM() : null));
    } else {
      proxy.moveProxy(lastPosC.getEl()
          .getDOM(), null);
    }

    scrollPos = portal.getBody()
        .getScroll();

    return "x-dd-drop-ok";
  }

  @Override
  public String notifyEnter(DragSource source, EventObject e, DragData data) {
    JsLibrary.showGwtContainerMask();
    JsLibrary.reduceGhostPanel();
    return super.notifyEnter(source, e, data);
  }

  @Override
  public void notifyOut(DragSource source, EventObject e, DragData data) {
    this.grid = null;
    JsLibrary.hideGwtContainerMask();
  }

  @Override
  public boolean notifyDrop(DragSource source, EventObject e, DragData data) {
    GadgetPortlet gp = portal.getGadgetPortletById(source.getId());
    GadgetPosition dropPosition = portal.getDropPosition();
    GadgetBean bean = gp.getGadgetBean();
    PortalColumn dragCol = portal.getPortalColumn(bean.getGadgetPosition()
        .getPlaceID());
    PortalColumn dropCol = portal.getPortalColumn(dropPosition.getPlaceID());
    saveDropZone(bean, dropPosition, dragCol, dropCol);
    JsLibrary.removePossibleColumn();

    grid = null;
    if (lastPosC == null)
      return false;

    PanelProxy proxy = new PanelProxy(source.getProxy()
        .getJsObj());

    proxy.getProxy()
        .remove();
    lastPosC.remove(gp.getId());
    lastPosC.insert(dropPosition.getPosition(), gp);
    JsLibrary.hideAndShowGadget(gp.getId());
    lastPosC.doLayout();

    final int scrollTop = scrollPos[0];
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        if (scrollPos != null)
          portal.getBody()
              .setScrollTop(scrollTop);
        portal.doLayout();
      }
    });
    lastPosC = null;

    JsLibrary.hideGwtContainerMask();
    return true;
  }

  private PosGrid[] getGrid() {
    Component[] items = portal.getItems();
    if (portal.getContainer()
        .getLayout()
        .contains(DEFAULT))
      return getDefaultGrid(items);
    else
      return getComplexGrid(items);
  }

  private PosGrid[] getDefaultGrid(Component[] items) {
    PosGrid[] posGrid = new PosGrid[items.length];
    for (int i = 0; i < items.length; i++) {
      ExtElement elem = items[i].getEl();
      posGrid[i] = new DefaultPosGrid(elem.getX(), elem.getWidth());
    }
    return posGrid;
  }

  private PosGrid[] getComplexGrid(Component[] items) {
    PosGrid[] posGrid = new PosGrid[items.length];
    for (int i = 0; i < items.length; i++) {
      ExtElement elem = items[i].getEl();
      posGrid[i] = new ComplexPosGrid(elem.getX(), elem.getWidth(),
          elem.getY(), elem.getHeight());
    }
    return posGrid;
  }

  private void saveDropZone(GadgetBean bean, GadgetPosition dropPosition,
      PortalColumn dragCol, PortalColumn dropCol) {
    bean.setPosition(dropPosition);
    ArrayList<GadgetBean> beans = getOrderingAndUpdatingBeans(dragCol, bean);
    beans.addAll(getOrderingAndUpdatingBeans(dropCol, bean));

    ContainerEntryPoint.getService()
        .saveGadgetPosition(beans, ContainerEntryPoint.getGwtParams(),
            new SaveGadgetAsyncCallback());
  }

  private ArrayList<GadgetBean> getOrderingAndUpdatingBeans(PortalColumn col,
      GadgetBean bean) {
    ArrayList<GadgetBean> gadgets = new ArrayList<GadgetBean>();
    NodeList<Node> childs = col.getElement()
        .getChildNodes();
    for (int i = 0; i < childs.getLength(); i++) {
      GadgetPortlet p = portal.getGadgetPortletById(Element.as(
          childs.getItem(i))
          .getId());
      if (p != null) {
        GadgetBean b = p.getGadgetBean();
        if (!bean.getRef()
            .equals(b.getRef())) {
          b.getGadgetPosition()
              .setPosition(i);
        }
        gadgets.add(b);
      }
    }
    return gadgets;
  }
}
