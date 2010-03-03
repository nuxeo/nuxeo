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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.ExtElement;
import com.gwtext.client.dd.DragData;
import com.gwtext.client.dd.DragSource;
import com.gwtext.client.dd.DropTargetConfig;
import com.gwtext.client.dd.ScrollManager;
import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.Container;
import com.gwtext.client.widgets.portal.PanelProxy;
import com.gwtext.client.widgets.portal.PortalColumn;
import com.gwtext.client.widgets.portal.PortalDropZone;

/**
 * DropZone serve for catch drag and drop event and call container service for
 * save
 * 
 * @author Guillaume Cusnieux
 * 
 */
public class DropZone extends PortalDropZone {

    private static final String DEFAULT = "default";

    private static final String ZONE_CLASS = "x-column-possible";

    private static final int GHOST_HEIGHT = 100;

    private static final int GHOST_WIDTH = 140;

    private static ContainerPortal portal;

    private int lastCW = -1;

    private PosGrid[] grid;

    private Container lastPosC;

    private int col;

    public DropZone(ContainerPortal portal, DropTargetConfig config) {
        super(portal, config);
        ScrollManager.register(portal.getBody()
                .getDOM());
        DropZone.portal = portal;
        overrideDragDrop(GHOST_WIDTH, GHOST_HEIGHT, ZONE_CLASS);
    }

    private static native void overrideDragDrop(int defaultX, int defaultY,
            String className)
    /*-{
      var _W;
      $wnd.Ext.override($wnd.Ext.Panel.DD, {
        startDrag : function(x,y){
          var g = this.proxy.getGhost();
          var h = g.getWidth();

          var X = x - 70;
          g.setX(X);
          g.setWidth(140);

          if(g.getHeight() > 50)
            g.setHeight(100);

          $wnd.Ext.select("div.x-column").addClass("x-column-possible");
          g.select("div.x-tool").setStyle("display","none");
          g.select("div.x-panel-tl").setStyle("background-color","#a29f9a");
        },

        alignElWithMouse: function(el, iPageX, iPageY) {
          var fly = el.dom ? el : $wnd.Ext.fly(el, '_dd');
          var X = iPageX - 70;
          var Y = iPageY - 10;
          fly.setLeftTop(X, Y);
          this.cachePosition(X, Y);
          this.autoScroll(X, Y, el.offsetHeight, el.offsetWidth);
          _W = this.proxy.proxy.getWidth();
          return this.getTargetCoord(X, Y);
        },

        endDrag : function(e){
          this.proxy.hide();
          this.panel.saveState();
          $wnd.Ext.select("div.x-column").removeClass("x-column-possible");
          $wnd.Ext.select("div.x-tool").setStyle("display","block");
        }
      });

      $wnd.Ext.override($wnd.Ext.dd.PanelProxy, {
        hide : function(){
          if(this.ghost) {
            var el = this.panel.el;
            el.dom.style.display = '';
            el.setWidth(this.ghost.getWidth());
            var w = el.getWidth();
            if(this.proxy) {
              this.proxy.remove();
              delete this.proxy;
            }
            el.setWidth(_W,true);
            this.ghost.remove();
            delete this.ghost;
          }
        }
      });

      $wnd.Ext.override($wnd.Ext.dd.DragSource, {
        onDragDrop : function(e, id){
          var target = this.cachedTarget || $wnd.Ext.dd.DragDropMgr.getDDById(id);
          if(this.beforeDragDrop(target, e, id) !== false){
            if(target.isNotifyTarget){
              if(target.notifyDrop(this, e, this.dragData))
                this.onValidDrop(target, e, id);
              else
                this.onValidDrop(target, e, id);
              }else
                this.onValidDrop(target, e, id);
              if(this.afterDragDrop)
                this.afterDragDrop(target, e, id);
            }
          delete this.cachedTarget;
        },

        onDragOut : function(e, id){
          var target = this.cachedTarget || $wnd.Ext.dd.DragDropMgr.getDDById(id);
          target.notifyOut(this, e, this.dragData);
        },

        onInvalidDrop : function(target, e, id){
          this.onDragDrop(e,id);
        }

      });
    }-*/;

    Timer t;

    @Override
    public String notifyOver(final DragSource source, final EventObject e,
            DragData data) {

        if (t != null)
            t.cancel();

        t = new Timer() {
            @Override
            public void run() {
                _notifyEnter(source, e);
            }
        };

        t.schedule(60);

        return "x-dd-drop-ok";
    }

    private void _notifyEnter(final DragSource source, final EventObject e) {
        int[] xy = e.getXY();
        PanelProxy proxy = new PanelProxy(source.getProxy()
                .getJsObj());

        if (grid == null) {
            grid = getGrid();
        }
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
    }

    @Override
    public String notifyEnter(DragSource source, EventObject e, DragData data) {
        JsLibrary.showGwtContainerMask();
        return super.notifyEnter(source, e, data);
    }

    @Override
    public void notifyOut(DragSource source, EventObject e, DragData data) {
        this.grid = null;
        JsLibrary.hideGwtContainerMask();
        this.notifyEnter(source, e, data);
    }

    @Override
    public boolean notifyDrop(final DragSource source, EventObject e,
            DragData data) {
        final GadgetPortlet gp = portal.getGadgetPortlet(source.getId());
        gp.reloadRenderUrl();
        final GadgetPosition dropPosition = portal.getDropPosition();
        final GadgetBean bean = gp.getGadgetBean();
        if (dropPosition != null) {
            PortalColumn dragCol = portal.getPortalColumn(bean.getGadgetPosition()
                    .getPlaceID());
            PortalColumn dropCol = portal.getPortalColumn(dropPosition.getPlaceID());
            saveDropZone(bean, dropPosition, dragCol, dropCol);
        }

        grid = null;

        PanelProxy proxy = new PanelProxy(source.getProxy()
                .getJsObj());

        proxy.getProxy()
                .remove();

        if (lastPosC != null) {
            if (dropPosition != null) {
                lastPosC.remove(gp.getId());
                lastPosC.insert(bean.getGadgetPosition()
                        .getPosition(), gp);
            }
            lastPosC.doLayout();
            lastPosC = null;
        }
        JsLibrary.hideGwtContainerMask();
        gp.renderDefaultPreferences();
        Timer t = new Timer() {

            @Override
            public void run() {
                gp.removeStyle();
            }

        };

        t.schedule(500);

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

    private static void saveDropZone(GadgetBean bean,
            GadgetPosition dropPosition, PortalColumn dragCol,
            PortalColumn dropCol) {
        bean.setPosition(dropPosition);
        ArrayList<GadgetBean> beans = getOrderingAndUpdatingBeans(dragCol, bean);
        beans.addAll(getOrderingAndUpdatingBeans(dropCol, bean));
        ContainerEntryPoint.getService()
                .saveGadgetsCollection(beans,
                        ContainerEntryPoint.getGwtParams(),
                        new AsyncCallback<Boolean>() {

                            public void onFailure(Throwable arg0) {
                            }

                            public void onSuccess(Boolean arg0) {
                            }
                        });
    }

    private static ArrayList<GadgetBean> getOrderingAndUpdatingBeans(
            PortalColumn col, GadgetBean bean) {
        ArrayList<GadgetBean> gadgets = new ArrayList<GadgetBean>();
        NodeList<Node> childs = col.getElement()
                .getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            GadgetPortlet p = portal.getGadgetPortlet(Element.as(
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
