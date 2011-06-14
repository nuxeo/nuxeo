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

package org.nuxeo.opensocial.container.client.presenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.opensocial.container.client.AppInfoMessages;
import org.nuxeo.opensocial.container.client.Container;
import org.nuxeo.opensocial.container.client.ContainerConfiguration;
import org.nuxeo.opensocial.container.client.event.priv.app.HideMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.ClosePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.ClosePortletEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.CollapsePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.CollapsePortletEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.MaximizePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.MaximizePortletEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.MinimizePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.MinimizePortletEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.SetPreferencesPortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.SetPreferencesPortletEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.UncollapsePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.UncollapsePortletEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.ContainerSizeChangedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ContainerSizeChangedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.FooterSelectedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.FooterSelectedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.HeaderSelectedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.HeaderSelectedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.SideBarChangedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.SideBarChangedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentAddedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentAddedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentIdChangedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentIdChangedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentMovedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentMovedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentRemovedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentRemovedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneAddedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneAddedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneDeletedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneRowDeletedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneUpdatedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneUpdatedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.service.WebContentsLoadedEvent;
import org.nuxeo.opensocial.container.client.event.priv.service.WebContentsLoadedEventHandler;
import org.nuxeo.opensocial.container.client.external.opensocial.OpenSocialPresenter;
import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.client.model.adapter.GwtWebContentAdapter;
import org.nuxeo.opensocial.container.client.ui.api.HasId;
import org.nuxeo.opensocial.container.client.ui.api.HasUnits;
import org.nuxeo.opensocial.container.client.ui.api.HasWebContents;
import org.nuxeo.opensocial.container.client.view.PreferencesWidget;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;
import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractComponent;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUICustomBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.allen_sauer.gwt.dnd.client.DragEndEvent;
import com.allen_sauer.gwt.dnd.client.DragHandler;
import com.allen_sauer.gwt.dnd.client.DragStartEvent;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.Presenter;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

/**
 * @author Stéphane Fourrier
 */
public class ContainerPresenter extends
        WidgetPresenter<ContainerPresenter.Display> {

    private static final String CANVAS_VIEW = "canvas";

    protected static final String DEFAULT_VIEW = "default";

    public interface Display extends WidgetDisplay {
        void setContainerHeader(boolean headerEnabled, String headerId);

        HasId getContainerHeader();

        HasId getContainerFooter();

        HasId getContainerSideBar();

        void setContainerCustomSize(String cssStyle, long size);

        void setContainerFixedSize(String cssStyle);

        void setContainerSideBar(boolean hasSideBar, String cssStyle,
                String string);

        void setContainerFooter(boolean footerEnabled, String footerId);

        void setData();

        void addZone(String cssStyle);

        void removeZone(int zoneIndex);

        HasUnits getZone(int index);

        void updateZoneTemplate(int zoneIndex, String zoneClass);

        void addUnit(int zoneIndex, String unitClass, String unitId);

        void removeUnit(int zoneIndex, int unitIndex);

        HasWebContents getUnit(String unitId);

        HasWebContents getUnit(int zoneIndex, int unitIndex);

        void removeWebContent(String webContentId);

        WidgetDisplay getWebContent(String webContentId);

        void moveWebContents(int fromZoneIndex, int fromUnitIndex,
                int toZoneIndex, int toUnitIndex);

        void moveWebContent(String fromUnitId, int fromWebContentPosition,
                String toUnitId, int toWebContentPosition);

        void maximizeWebContent(Widget widget);

        void minimizeWebContent(Widget widget, String unitId, long position);
    }

    public static final Place PLACE = new Place("Container");

    public static AppInfoMessages infos = AppPresenter.infos;

    private AppModel model;

    private Map<String, YUIFlowPanelDropController> dropControllerList;

    private Map<String, Presenter> webContentPresenters;

    private PickupDragController dragController;

    public boolean isMaximized = false;

    @Inject
    public ContainerPresenter(Display display, EventBus eventBus, AppModel model) {
        super(display, eventBus);
        this.model = model;
        this.dropControllerList = new HashMap<String, YUIFlowPanelDropController>();
        this.webContentPresenters = new HashMap<String, Presenter>();

        fetchLayoutContent();
    }

    private void fetchLayoutContent() {
        dragController = new PickupDragController(Container.rootPanel, false);
        Container.rootPanel.getElement().getStyle().setPosition(Position.RELATIVE);
        registerDragAndDropEvent();
        dragController.setBehaviorConstrainedToBoundaryPanel(false);
        dragController.setBehaviorMultipleSelection(false);
        dragController.setBehaviorDragStartSensitivity(0);

        if (model.getLayout() != null) {
            setContainerSize();
            setLayoutZonesAndUnits();

            setContainerHeader();
            setContainerFooter();
            setContainerSideBar();

            if (!model.getLayout().getSidebarStyle().equals(
                    YUISideBarStyle.YUI_SB_NO_COLUMN)) {
                addDropControllerFor(model.getLayout().getSideBar());
            }

            for (YUIComponent zone : model.getLayout().getContent().getComponents()) {
                for (YUIComponent unit : zone.getComponents()) {
                    addDropControllerFor((YUIUnit) unit);
                }
            }

            display.setData();
        }
    }

    private void setContainerHeader() {
        YUIUnit header = model.getLayout().getHeader();

        if (header == null) {
            String oldId = display.getContainerHeader().getId();
            display.setContainerHeader(false, "");
            removeDropControllerFor(oldId);
        } else {
            display.setContainerHeader(true, ((YUIComponent) header).getId());
            addDropControllerFor(header);
        }
    }

    private void setContainerFooter() {
        YUIUnit footer = model.getLayout().getFooter();
        if (footer == null) {
            String oldId = display.getContainerFooter().getId();
            display.setContainerFooter(false, "");
            removeDropControllerFor(oldId);
        } else {
            display.setContainerFooter(true, ((YUIComponent) footer).getId());
            addDropControllerFor(model.getLayout().getFooter());
        }
    }

    private void setLayoutZonesAndUnits() {
        int zoneIndex = 0;

        for (YUIComponent zoneComponent : model.getLayout().getContent().getComponents()) {
            display.addZone(zoneComponent.getCSS());
            for (YUIComponent unitComponent : zoneComponent.getComponents()) {
                display.addUnit(zoneIndex, unitComponent.getCSS(),
                        unitComponent.getId());
                display.updateZoneTemplate(zoneIndex, zoneComponent.getCSS());
            }
            zoneIndex++;
        }
    }

    private void setContainerSize() {
        YUIBodySize bodySize = model.getLayout().getBodySize();

        if (bodySize instanceof YUICustomBodySize) {
            display.setContainerCustomSize(bodySize.getCSS(),
                    bodySize.getSize());
        } else if (bodySize instanceof YUIFixedBodySize) {
            display.setContainerFixedSize(bodySize.getCSS());
        }
    }

    private void setContainerSideBar() {
        YUISideBarStyle sideBar = model.getLayout().getSidebarStyle();

        if (YUISideBarStyle.YUI_SB_NO_COLUMN.toString().equals(
                sideBar.toString())) {
            String oldId = display.getContainerSideBar().getId();
            display.setContainerSideBar(false, sideBar.getCSS(), "");
            removeDropControllerFor(oldId);
        } else {
            String sidebarId = ((YUIComponent) model.getLayout().getSideBar()).getId();
            display.setContainerSideBar(true, sideBar.getCSS(), sidebarId);
            addDropControllerFor(model.getLayout().getSideBar());
        }
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        registerBusEventSideBarChangement();
        registerBusEventHeaderSelection();
        registerBusEventFooterSelection();
        registerBusEventContainerSizeChangement();
        registerBusEventZoneAddition();
        registerBusEventZoneDeletion();
        registerBusEventZoneUpdate();
        registerBusEventWebContentAddition();
        registerBusEventWebContentRemoval();
        registerBusEventWebContentsLoad();
        registerBusEventWebContentMovement();
        registerBusEventWebContentIdChangement();
        registerBusEventPreferencesPortletSet();
        registerBusEventPorletCollapse();
        registerBusEventPorletUncollapse();
        registerBusEventWebContentPortletMaximization();
        registerBusEventWebContentPortletMinimization();
        registerBusEventWebContentPorletClose();
        registerBusEventWebContentPortletUpdate();
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
        fetchLayoutContent();
    }

    public void revealDisplay() {

    }

    private void registerBusEventSideBarChangement() {
        registerHandler(eventBus.addHandler(SideBarChangedEvent.TYPE,
                new SideBarChangedEventHandler() {
                    public void onChangeSideBarPosition(
                            SideBarChangedEvent event) {
                        setContainerSideBar();
                    }
                }));
    }

    private void registerBusEventHeaderSelection() {
        registerHandler(eventBus.addHandler(HeaderSelectedEvent.TYPE,
                new HeaderSelectedEventHandler() {
                    public void onSelectHeader(HeaderSelectedEvent event) {
                        setContainerHeader();
                    }
                }));
    }

    private void registerBusEventFooterSelection() {
        registerHandler(eventBus.addHandler(FooterSelectedEvent.TYPE,
                new FooterSelectedEventHandler() {
                    public void onSelectFooter(FooterSelectedEvent event) {
                        setContainerFooter();
                    }
                }));
    }

    private void registerBusEventContainerSizeChangement() {
        registerHandler(eventBus.addHandler(ContainerSizeChangedEvent.TYPE,
                new ContainerSizeChangedEventHandler() {
                    public void onChangeContainerSize(
                            ContainerSizeChangedEvent event) {
                        setContainerSize();
                    }
                }));
    }

    private void registerBusEventZoneAddition() {
        registerHandler(eventBus.addHandler(ZoneAddedEvent.TYPE,
                new ZoneAddedEventHandler() {
                    public void onAddRow(ZoneAddedEvent event) {
                        int zoneIndex = model.getLayout().getContent().getComponents().size() - 1;
                        YUIComponentZoneImpl zoneComponent = (YUIComponentZoneImpl) model.getLayout().getContent().getComponents().get(
                                zoneIndex);
                        display.addZone(zoneComponent.getCSS());

                        for (YUIComponent unitComponent : zoneComponent.getComponents()) {
                            String idRef = unitComponent.getId();

                            display.addUnit(zoneIndex, unitComponent.getCSS(),
                                    idRef);

                            addDropControllerFor((YUIUnit) unitComponent);
                        }

                        display.updateZoneTemplate(zoneIndex,
                                zoneComponent.getCSS());
                    }
                }));
    }

    private void registerBusEventZoneDeletion() {
        registerHandler(eventBus.addHandler(ZoneDeletedEvent.TYPE,
                new ZoneRowDeletedEventHandler() {
                    public void onRowDeleted(ZoneDeletedEvent event) {
                        for (int i = 0; i < display.getZone(event.getId()).getNumberOfUnits(); i++) {
                            removeDropControllerFor(((HasId) display.getUnit(
                                    event.getId(), i)).getId());
                        }
                        display.removeZone(event.getId());
                    }
                }));
    }

    private void registerBusEventZoneUpdate() {
        registerHandler(eventBus.addHandler(ZoneUpdatedEvent.TYPE,
                new ZoneUpdatedEventHandler() {
                    public void onUpdateRow(ZoneUpdatedEvent event) {
                        YUIComponentZoneImpl zoneComponent = ((YUIComponentZoneImpl) model.getLayout().getContent().getComponents().get(
                                event.getId()));

                        HasUnits zone = display.getZone(event.getId());

                        int actualNumberOfUnits = zone.getNumberOfUnits();
                        int wantedNumberOfUnits = zoneComponent.getTemplate().getNumberOfComponents();

                        if (actualNumberOfUnits < wantedNumberOfUnits) {
                            for (int i = actualNumberOfUnits; i < wantedNumberOfUnits; i++) {
                                YUIUnitImpl unit = (YUIUnitImpl) zoneComponent.getComponents().get(
                                        i);

                                display.addUnit(event.getId(), unit.getCSS(),
                                        unit.getId());

                                addDropControllerFor(unit);
                            }
                        } else if (actualNumberOfUnits > wantedNumberOfUnits) {
                            for (int i = actualNumberOfUnits - 1; i > wantedNumberOfUnits - 1; i--) {
                                removeDropControllerFor(((HasId) display.getUnit(
                                        event.getId(), i)).getId());

                                display.removeUnit(event.getId(), i);
                            }
                        }
                        display.updateZoneTemplate(event.getId(),
                                zoneComponent.getCSS());
                    }
                }));
    }

    private void registerBusEventWebContentAddition() {
        registerHandler(eventBus.addHandler(WebContentAddedEvent.TYPE,
                new WebContentAddedEventHandler() {
                    public void onAddWebContent(WebContentAddedEvent event) {
                        GwtWebContentAdapter webContentAdapter = new GwtWebContentAdapter(
                                event.getAbstractData(), eventBus);

                        Presenter webContentPresenter = webContentAdapter.getContainerPresenter();
                        WidgetDisplay webContentView = (WidgetDisplay) webContentPresenter.getDisplay();
                        WebContentData webContentData = event.getAbstractData();

                        webContentPresenters.put(webContentData.getId(),
                                webContentPresenter);

                        webContentPresenter.bind();
                        webContentPresenter.revealDisplay();

                        HasWebContents unit = display.getUnit(webContentData.getUnitId());
                        if (unit != null) {
                            unit.addWebContent(webContentView.asWidget());
                        }

                        if (webContentData.isInAPorlet()) {
                            makeDraggable(webContentData, webContentView);
                            if (ContainerConfiguration.showPreferencesAfterAddingGadget()) {
                                eventBus.fireEvent(new SetPreferencesPortletEvent(
                                        webContentData.getId()));
                            }
                        }
                    }
                }));
    }

    private void makeDraggable(WebContentData webContentData,
            WidgetDisplay display) {
        String spaceId = model.getContainerContext().getSpaceId();
        if (webContentData.isInAPorlet()
                && model.hasPermission(spaceId, PermissionsConstants.EVERYTHING)) {
            dragController.makeDraggable(display.asWidget(),
                    ((PortletPresenter.Display) display).getHeader());
        }
    }

    private void makeNotDraggable(WebContentData webContentData,
            WidgetDisplay display) {
        String spaceId = model.getContainerContext().getSpaceId();
        if (webContentData.isInAPorlet()
                && model.hasPermission(spaceId, PermissionsConstants.EVERYTHING)) {
            dragController.makeNotDraggable(display.asWidget());
        }
    }

    private void registerBusEventWebContentRemoval() {
        registerHandler(eventBus.addHandler(WebContentRemovedEvent.TYPE,
                new WebContentRemovedEventHandler() {
                    public void onWebContentRemoved(WebContentRemovedEvent event) {
                        webContentPresenters.get(event.getWebContentId()).unbind();
                    }
                }));
    }

    private void registerBusEventWebContentsLoad() {
        registerHandler(eventBus.addHandler(WebContentsLoadedEvent.TYPE,
                new WebContentsLoadedEventHandler() {
                    public void onWebContentsLoaded(WebContentsLoadedEvent event) {
                        for (Entry<String, List<WebContentData>> webContents : model.getWebContents().entrySet()) {
                            for (WebContentData webContent : webContents.getValue()) {
                                GwtWebContentAdapter webContentAdapter = new GwtWebContentAdapter(
                                        webContent, eventBus);

                                Presenter webContentPresenter = webContentAdapter.getContainerPresenter();
                                WidgetDisplay webContentView = (WidgetDisplay) webContentPresenter.getDisplay();

                                webContentPresenters.put(webContent.getId(),
                                        webContentPresenter);

                                makeDraggable(webContent, webContentView);

                                webContentPresenter.bind();
                                webContentPresenter.revealDisplay();

                                HasWebContents unit = display.getUnit(webContent.getUnitId());
                                if (unit != null) {

                                    unit.addWebContent(webContentView.asWidget());
                                }
                            }
                        }

                        eventBus.fireEvent(new HideMessageEvent());
                    }
                }));
    }

    private void registerBusEventWebContentMovement() {
        registerHandler(eventBus.addHandler(WebContentMovedEvent.TYPE,
                new WebContentMovedEventHandler() {
                    public void onWebContentHasMoved(WebContentMovedEvent event) {
                        display.moveWebContent(event.getFromUnitName(),
                                event.getFromWebContentPosition(),
                                event.getToUnitName(),
                                event.getToWebContentPosition());
                    }
                }));
    }

    private void registerBusEventWebContentIdChangement() {
        registerHandler(eventBus.addHandler(WebContentIdChangedEvent.TYPE,
                new WebContentIdChangedEventHandler() {
                    public void onWebContentIdChange(
                            WebContentIdChangedEvent event) {
                        ((HasId) display.getWebContent(event.getOldWebContentId())).setId(event.getNewWebContentId());
                        webContentPresenters.put(event.getNewWebContentId(),

                        webContentPresenters.remove(event.getOldWebContentId()));
                    }
                }));
    }

    private void registerBusEventPreferencesPortletSet() {
        eventBus.addHandler(SetPreferencesPortletEvent.TYPE,
                new SetPreferencesPortletEventHandler() {
                    public void onSetPreferences(
                            SetPreferencesPortletEvent event) {
                        PreferencesWidget preferencesWidget = new PreferencesWidget();

                        PreferencesPresenter preferencesPresenter = new PreferencesPresenter(
                                preferencesWidget,
                                eventBus,
                                (PortletPresenter) webContentPresenters.get(event.getId()),
                                model);

                        preferencesPresenter.bind();
                        preferencesPresenter.revealDisplay();
                    }
                });
    }

    private void registerBusEventPorletCollapse() {
        eventBus.addHandler(CollapsePortletEvent.TYPE,
                new CollapsePortletEventHandler() {
                    public void onCollapsePortlet(CollapsePortletEvent event) {
                        PortletPresenter gadget = (PortletPresenter) webContentPresenters.get(event.getId());
                        gadget.collapse();
                        model.getWebContent(event.getId()).setIsCollapsed(true);
                        model.updateWebContent(event.getId(), null);
                    }
                });
    }

    private void registerBusEventPorletUncollapse() {
        eventBus.addHandler(UncollapsePortletEvent.TYPE,
                new UncollapsePortletEventHandler() {
                    public void onUncollapsePortlet(UncollapsePortletEvent event) {
                        PortletPresenter gadget = (PortletPresenter) webContentPresenters.get(event.getId());
                        gadget.uncollapse();
                        model.getWebContent(event.getId()).setIsCollapsed(false);
                        model.updateWebContent(event.getId(), null);
                    }
                });
    }

    private void registerBusEventWebContentPortletMaximization() {
        eventBus.addHandler(MaximizePortletEvent.TYPE,
                new MaximizePortletEventHandler() {
                    @SuppressWarnings("unchecked")
                    public void onMaximizeWebContent(MaximizePortletEvent event) {
                        if (!isMaximized) {
                            // TODO We have to handle the different view for the
                            // gadget. For the time, it is only handle by the
                            // OpenSocialPresenter !
                            WidgetPresenter presenter = (WidgetPresenter) webContentPresenters.get(event.getId());
                            if (presenter instanceof PortletPresenter) {
                                WidgetPresenter contentPresenter = (WidgetPresenter) ((PortletPresenter) presenter).getContentPresenter();
                                if (contentPresenter instanceof OpenSocialPresenter) {
                                    ((OpenSocialPresenter) contentPresenter).setView(CANVAS_VIEW);
                                }
                            }

                            WidgetDisplay gadgetView = (WidgetDisplay) presenter.getDisplay();

                            WebContentData webContent = model.getWebContent(event.getId());
                            makeNotDraggable(webContent, gadgetView);

                            display.setContainerSideBar(false,

                            YUISideBarStyle.YUI_SB_NO_COLUMN.getCSS(), "");
                            YUIUnit header = model.getLayout().getHeader();
                            YUIUnit footer = model.getLayout().getFooter();
                            if (header != null) {
                                display.setContainerHeader(
                                        false,
                                        ((YUIComponent) model.getLayout().getHeader()).getId());
                            }
                            if (footer != null) {
                                display.setContainerFooter(
                                        false,
                                        ((YUIComponent) model.getLayout().getFooter()).getId());
                            }

                            display.maximizeWebContent(gadgetView.asWidget());
                            isMaximized = true;
                        }
                    }
                });
    }

    private void registerBusEventWebContentPortletMinimization() {
        eventBus.addHandler(MinimizePortletEvent.TYPE,
                new MinimizePortletEventHandler() {
                    @SuppressWarnings("unchecked")
                    public void onMinimizeWebContent(MinimizePortletEvent event) {
                        if (isMaximized) {
                            // TODO We have to handle the different view for the
                            // gadget. For the time, it is only handle by the
                            // OpenSocialPresenter !
                            WidgetPresenter presenter = (WidgetPresenter) webContentPresenters.get(event.getId());
                            if (presenter instanceof PortletPresenter) {
                                WidgetPresenter contentPresenter = (WidgetPresenter) ((PortletPresenter) presenter).getContentPresenter();
                                if (contentPresenter instanceof OpenSocialPresenter) {
                                    ((OpenSocialPresenter) contentPresenter).setView(DEFAULT_VIEW);
                                }
                            }

                            WidgetDisplay gadgetView = ((WidgetDisplay) webContentPresenters.get(
                                    event.getId()).getDisplay());

                            WebContentData webContent = model.getWebContent(event.getId());
                            makeDraggable(webContent, gadgetView);

                            setContainerSideBar();
                            YUIUnit header = model.getLayout().getHeader();
                            YUIUnit footer = model.getLayout().getFooter();
                            if (header != null) {
                                display.setContainerHeader(true,
                                        ((YUIComponent) header).getId());
                            }
                            if (footer != null) {
                                display.setContainerFooter(true,
                                        ((YUIComponent) footer).getId());
                            }

                            display.minimizeWebContent(gadgetView.asWidget(),
                                    webContent.getUnitId(),
                                    webContent.getPosition());
                            isMaximized = false;
                        }
                    }
                });
    }

    private void registerBusEventWebContentPortletUpdate() {
        eventBus.addHandler(WebContentUpdatedEvent.TYPE,
                new WebContentUpdatedEventHandler() {
                    public void onWebContentUpdated(WebContentUpdatedEvent event) {
                        Presenter presenter = webContentPresenters.get(event.getWebContentId());
                        presenter.refreshDisplay();
                    }
                });
    }

    private void registerBusEventWebContentPorletClose() {
        eventBus.addHandler(ClosePortletEvent.TYPE,
                new ClosePortletEventHandler() {
                    public void onCloseWebContent(ClosePortletEvent event) {
                        if (Window.confirm(infos.isSureToDeleteGadget())) {
                            model.removeWebContent(event.getId());
                        }
                    }
                });
    }

    private void addDropControllerFor(YUIUnit unit) {
        String spaceId = model.getContainerContext().getSpaceId();
        if (unit != null
                && model.hasPermission(spaceId, PermissionsConstants.EVERYTHING)) {
            String idRef = ((YUIAbstractComponent) unit).getId();
            FlowPanel unitWidget = (FlowPanel) display.getUnit(idRef);
            YUIFlowPanelDropController dropController = new YUIFlowPanelDropController(
                    unitWidget);

            dragController.registerDropController(dropController);
            dropControllerList.put(idRef, dropController);
        }
    }

    private void removeDropControllerFor(String idRef) {
        YUIFlowPanelDropController dropController = dropControllerList.get(idRef);
        String spaceId = model.getContainerContext().getSpaceId();
        if (dropController != null
                && model.hasPermission(spaceId, PermissionsConstants.EVERYTHING)) {
            dragController.unregisterDropController(dropController);
            dropControllerList.remove(idRef);
        }
    }

    private void registerDragAndDropEvent() {
        dragController.addDragHandler(new DragHandler() {
            private int webContentPositionBeforeDragging;

            private String webContentUnitIdBeforeDragging;

            public void onPreviewDragStart(DragStartEvent event)
                    throws VetoDragException {
            }

            public void onPreviewDragEnd(DragEndEvent event)
                    throws VetoDragException {
            }

            public void onDragStart(DragStartEvent event) {
                PortletPresenter.Display webContentView = (PortletPresenter.Display) event.getSource();
                webContentUnitIdBeforeDragging = webContentView.getParentId();
                int i = 0;
                for (WebContentData webContent : model.getWebContents().get(
                        webContentUnitIdBeforeDragging)) {
                    String webContentId = webContentView.getId();
                    if (webContent.getId().equals(webContentId)) {
                        webContentPositionBeforeDragging = i;
                    }

                    i++;
                }

                highLightUnits();
                ((PortletPresenter) webContentPresenters.get(webContentView.getId())).hideContent();
            }

            public void onDragEnd(DragEndEvent event) {
                PortletPresenter.Display webContentView = (PortletPresenter.Display) event.getSource();
                String webContentUnitIdAfterDragging = webContentView.getParentId();
                int webContentPositionAfterDragging = display.getUnit(
                        webContentUnitIdAfterDragging).getWebContentPosition(
                        webContentView.asWidget());

                model.webContentMoved(webContentUnitIdBeforeDragging,
                        webContentPositionBeforeDragging,
                        webContentUnitIdAfterDragging,
                        webContentPositionAfterDragging);

                unHighLightUnits();

                ((PortletPresenter) webContentPresenters.get(webContentView.getId())).showContent();
            }

            private native void highLightUnits() /*-{
                                                 $wnd.jQuery('.yui-unit').addClass('highLight');
                                                 }-*/;

            private native void unHighLightUnits() /*-{
                                                   $wnd.jQuery('.yui-unit').removeClass('highLight');
                                                   }-*/;
        });
    }
}
