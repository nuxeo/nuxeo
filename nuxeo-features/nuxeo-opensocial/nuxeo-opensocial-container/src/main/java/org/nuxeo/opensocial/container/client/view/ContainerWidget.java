/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.presenter.ContainerPresenter;
import org.nuxeo.opensocial.container.client.ui.UnitWidget;
import org.nuxeo.opensocial.container.client.ui.ZoneWidget;
import org.nuxeo.opensocial.container.client.ui.api.HasId;
import org.nuxeo.opensocial.container.client.ui.api.HasUnits;
import org.nuxeo.opensocial.container.client.ui.api.HasWebContents;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import net.customware.gwt.presenter.client.widget.WidgetDisplay;

/**
 * @author Stéphane Fourrier
 */
public class ContainerWidget extends Composite implements
        ContainerPresenter.Display {
    private static final long serialVersionUID = 1L;

    private FlowPanel GWTLayout;

    private UnitWidget headerPanel;

    private FlowPanel contentPanel;

    private FlowPanel mainPanel;

    private SimplePanel maximizedContainer;

    private SimplePanel maximizedContainerContent;

    private FlowPanel subMainPanel;

    private UnitWidget sideBarPanel;

    private UnitWidget footerPanel;

    public ContainerWidget() {
        GWTLayout = new FlowPanel();
        GWTLayout.setVisible(false);
        initWidget(GWTLayout);

        headerPanel = new UnitWidget("yui-header");

        GWTLayout.add(headerPanel);

        contentPanel = new FlowPanel();
        contentPanel.getElement().setAttribute("id", "bd");
        GWTLayout.add(contentPanel);

        mainPanel = new FlowPanel();
        mainPanel.getElement().setAttribute("id", "yui-main");
        contentPanel.add(mainPanel);

        subMainPanel = new FlowPanel();
        subMainPanel.addStyleName("yui-b");
        mainPanel.add(subMainPanel);

        maximizedContainer = new SimplePanel();
        maximizedContainer.addStyleName("yui-b");
        maximizedContainer.setVisible(false);
        mainPanel.add(maximizedContainer);

        maximizedContainerContent = new SimplePanel();
        maximizedContainerContent.setStyleName("yui-u first yui-unit");
        maximizedContainer.add(maximizedContainerContent);

        sideBarPanel = new UnitWidget("yui-sideBar");
        sideBarPanel.addStyleName("yui-unit");
        sideBarPanel.addStyleName("yui-b");
        contentPanel.add(sideBarPanel);

        footerPanel = new UnitWidget("yui-footer");

        GWTLayout.add(footerPanel);
    }

    public void setContainerHeader(boolean hasHeader, String headerId) {
        headerPanel.setVisible(hasHeader);
        if (headerId != null) {
            headerPanel.setId(headerId);
        }
    }

    public HasId getContainerHeader() {
        return headerPanel;
    }

    public void setContainerCustomSize(String cssStyle, long size) {
        GWTLayout.getElement().setAttribute("id", cssStyle);
        GWTLayout.getElement().getStyle().setWidth(size, Unit.PX);
    }

    public void setContainerFixedSize(String cssStyle) {
        GWTLayout.getElement().setAttribute("id", cssStyle);
        GWTLayout.getElement().getStyle().clearWidth();
    }

    public void setContainerSideBar(boolean hasSidebar, String cssStyle,
            String sideBarId) {
        sideBarPanel.setVisible(hasSidebar);
        GWTLayout.setStyleName(cssStyle);
        sideBarPanel.setId(sideBarId);
    }

    public HasId getContainerSideBar() {
        return sideBarPanel;
    }

    public void setContainerFooter(boolean hasFooter, String footerId) {
        footerPanel.setVisible(hasFooter);
        if (footerId != null) {
            footerPanel.setId(footerId);
        }
    }

    public HasId getContainerFooter() {
        return footerPanel;
    }

    public void setData() {
        GWTLayout.setVisible(true);
    }

    public void addZone(String cssStyle) {
        ZoneWidget zone = new ZoneWidget(cssStyle);
        subMainPanel.add(zone);
    }

    public void removeZone(int zoneIndex) {
        subMainPanel.remove(zoneIndex);
    }

    public HasUnits getZone(int index) {
        return (HasUnits) subMainPanel.getWidget(index);
    }

    public void updateZoneTemplate(int zoneIndex, String zoneClass) {
        ((ZoneWidget) subMainPanel.getWidget(zoneIndex)).setCssTemplate(zoneClass);
    }

    public int getNumberOfZones() {
        return subMainPanel.getWidgetCount();
    }

    public void addUnit(int zoneIndex, String unitClass, String unitName) {
        UnitWidget unitWidget = new UnitWidget(unitClass);
        unitWidget.setId(unitName);
        ((ZoneWidget) subMainPanel.getWidget(zoneIndex)).addUnit(unitWidget);
    }

    public void removeUnit(int zoneIndex, int unitIndex) {
        ((ZoneWidget) subMainPanel.getWidget(zoneIndex)).removeUnit(unitIndex);
    }

    public HasWebContents getUnit(String unitName) {
        for (int i = 0; i < subMainPanel.getWidgetCount(); i++) {
            UnitWidget unitWidget = ((ZoneWidget) subMainPanel.getWidget(i)).getUnit(unitName);
            if (unitWidget != null) {
                return unitWidget;
            }
        }

        if (unitName.equals(headerPanel.getId()))
            return headerPanel;

        if (unitName.equals(footerPanel.getId()))
            return footerPanel;

        if (unitName.equals(sideBarPanel.getId()))
            return sideBarPanel;

        return null;
    }

    public HasWebContents getUnit(int zoneIndex, int unitIndex) {
        return ((ZoneWidget) subMainPanel.getWidget(zoneIndex)).getUnit(unitIndex);
    }

    public int getNumberOfUnits(int zoneIndex) {
        return ((HasUnits) subMainPanel.getWidget(zoneIndex)).getNumberOfUnits();
    }

    public void removeWebContent(String webContentId) {
        ((Widget) getWebContent(webContentId)).removeFromParent();
    }

    public WidgetDisplay getWebContent(String webContentId) {
        for (int zoneIndex = 0; zoneIndex < subMainPanel.getWidgetCount(); zoneIndex++) {
            for (UnitWidget unit : ((ZoneWidget) subMainPanel.getWidget(zoneIndex)).getUnits()) {
                HasId webContent = unit.getWebContent(webContentId);
                if (webContent != null) {
                    return (WidgetDisplay) webContent;
                }
            }
        }
        return null;
    }

    public void moveWebContents(int fromZoneIndex, int fromUnitIndex,
            int toZoneIndex, int toUnitIndex) {
        for (Widget webContent : getUnit(fromZoneIndex, fromUnitIndex).getWebContents()) {
            webContent.removeFromParent();
            getUnit(toZoneIndex, toUnitIndex).addWebContent(webContent);
        }
    }

    public void moveWebContent(String fromUnitName, int fromWebContentPosition,
            String toUnitName, int toWebContentPosition) {
        Widget webContent = getUnit(fromUnitName).getWebContent(
                fromWebContentPosition);
        webContent.removeFromParent();
        getUnit(toUnitName).addWebContent(webContent, toWebContentPosition);
    }

    public void maximizeWebContent(Widget widget) {
        if (!maximizedContainer.isVisible()) {
            switchViewFromContainerToCanvas();
            maximizedContainerContent.add(widget);
        }
    }

    public void minimizeWebContent(Widget widget, String unitName, long position) {
        if (maximizedContainer.isVisible()) {
            switchViewFromCanvasToContainer();
            getUnit(unitName).addWebContent(widget, position);
        }
    }

    public boolean hasWebContentInUnit(int zoneIndex, int unitIndex) {
        return ((ZoneWidget) subMainPanel.getWidget(zoneIndex)).getUnit(
                unitIndex).hasWebContents();
    }

    public boolean hasWebContentInUnit(String unitName) {
        return getUnit(unitName).hasWebContents();
    }

    public boolean hasWebContentInZone(int zoneIndex) {
        return ((HasWebContents) subMainPanel.getWidget(zoneIndex)).hasWebContents();
    }

    public boolean hasWebContentsIContainer() {
        for (int i = 0; i < subMainPanel.getWidgetCount(); i++) {
            if (((HasWebContents) subMainPanel.getWidget(i)).hasWebContents()) {
                return true;
            }
        }
        return false;
    }

    private void switchViewFromCanvasToContainer() {
        maximizedContainer.setVisible(false);
        subMainPanel.setVisible(true);
    }

    private void switchViewFromContainerToCanvas() {
        subMainPanel.setVisible(false);
        maximizedContainer.setVisible(true);
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
