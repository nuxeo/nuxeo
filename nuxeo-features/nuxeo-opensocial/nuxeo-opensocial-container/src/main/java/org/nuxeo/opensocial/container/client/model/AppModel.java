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

package org.nuxeo.opensocial.container.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.customware.gwt.presenter.client.EventBus;

import org.nuxeo.opensocial.container.client.AppErrorMessages;
import org.nuxeo.opensocial.container.client.ContainerConfiguration;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ContainerSizeChangedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.FooterSelectedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.HeaderSelectedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.SideBarChangedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentAddedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.WebContentRemovedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneAddedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneDeletedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneUpdatedEvent;
import org.nuxeo.opensocial.container.client.event.priv.service.LayoutLoadedEvent;
import org.nuxeo.opensocial.container.client.event.priv.service.WebContentsLoadedEvent;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.InitApplication;
import org.nuxeo.opensocial.container.client.rpc.InitApplicationResult;
import org.nuxeo.opensocial.container.client.rpc.layout.action.CreateYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.action.DeleteYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutBodySize;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutFooter;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutHeader;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutSideBar;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.result.CreateYUIZoneResult;
import org.nuxeo.opensocial.container.client.rpc.layout.result.DeleteYUIZoneResult;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutBodySizeResult;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutFooterResult;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutHeaderResult;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutSideBarResult;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUIZoneResult;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.CreateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.DeleteWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.UpdateAllWebContents;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.UpdateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.CreateWebContentResult;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.DeleteWebContentResult;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateAllWebContentsResult;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateWebContentResult;
import org.nuxeo.opensocial.container.client.utils.Severity;
import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractComponent;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.inject.Inject;

/**
 * @author Stéphane Fourrier
 */
public class AppModel implements HasPermissionsMapper {
    private AppErrorMessages errors = AppPresenter.errors;

    private static final YUITemplate defaultNewZoneTemplate = YUITemplate.YUI_ZT_100;

    public static final String GENERATE_TITLE_PARAMETER_NAME = "generateTitle";

    private static final String EVERYTHING = "Everything";

    private YUILayout layout;

    private Map<String, List<WebContentData>> webContents;

    private Map<String, Map<String, Boolean>> permissions;

    private EventBus eventBus;

    private ContainerDispatchAsync dispatcher;

    private ContainerContext containerContext;

    @Inject
    public AppModel(final EventBus eventBus,
            final ContainerDispatchAsync dispatcher) {
        this.eventBus = eventBus;
        this.dispatcher = dispatcher;

        webContents = new HashMap<String, List<WebContentData>>();
        permissions = new HashMap<String, Map<String, Boolean>>();
    }

    public ContainerContext getContainerContext() {
        return containerContext;
    }

    public Map<String, List<WebContentData>> getWebContents() {
        return webContents;
    }

    public void setWebContents(Map<String, List<WebContentData>> webContents) {
        this.webContents = webContents;
    }

    public WebContentData getWebContent(String webcontentId) {
        for (Entry<String, List<WebContentData>> unit : webContents.entrySet()) {
            int position = 0;
            for (WebContentData webcontent : unit.getValue()) {
                if (webcontent.getId().equals(webcontentId)) {
                    webcontent.setUnitId(unit.getKey());
                    webcontent.setPosition(position);
                    return webcontent;
                }

                position++;
            }
        }
        return null;
    }

    public YUILayout getLayout() {
        // Cache Layout
        if (layout == null) {
            initApplicationService();
        }
        return layout;
    }

    public void setLayout(YUILayout layout) {
        this.layout = layout;
    }

    public void setPermissions(Map<String, Map<String, Boolean>> permissions) {
        this.permissions = permissions;
    }

    // TODO STUB :Will be improved when the rights will be implemented in a more
    // complex way (with unit & webContents rights)
    public Boolean hasPermission(String id, String permission) {
        if (permissions.containsKey(id)
                && permissions.get(id).containsKey(EVERYTHING)
                && permissions.get(id).get(EVERYTHING).equals(Boolean.TRUE)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public Map<String, Map<String, Boolean>> getPermissions() {
        return permissions;
    }

    private boolean hasWebContentInZone(int zoneIndex) {
        for (YUIComponent unit : layout.getContent().getComponents().get(
                zoneIndex).getComponents()) {
            if (hasWebContentInUnit(unit.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWebContentInUnit(String unitName) {
        if (webContents.containsKey(unitName)) {
            return !webContents.get(unitName).isEmpty();
        }
        return false;
    }

    public void setBodySize(YUIBodySize bodySize) {
        this.layout.setBodySize(bodySize);

        updateLayoutBodySizeService();
    }

    public boolean setSideBar(YUISideBarStyle sideBar) {
        if (sideBar.equals(YUISideBarStyle.YUI_SB_NO_COLUMN)
                && hasWebContentInUnit(((YUIComponent) getLayout().getSideBar()).getId())) {
            eventBus.fireEvent(new SendMessageEvent(errors.zoneIsNotEmpty(),
                    Severity.ERROR));
            return Boolean.FALSE;
        } else {
            updateLayoutSideBarService(sideBar);

            return Boolean.TRUE;
        }
    }

    public boolean setHasHeader(boolean hasHeader) {
        YUIUnit header = getLayout().getHeader();
        if (header != null
                && hasWebContentInUnit(((YUIAbstractComponent) header).getId())) {
            eventBus.fireEvent(new SendMessageEvent(errors.zoneIsNotEmpty(),
                    Severity.ERROR));
            return Boolean.FALSE;
        } else {
            if (hasHeader) {
                updateLayoutHeaderService(new YUIUnitImpl());
            } else {
                updateLayoutHeaderService(null);
            }

            return Boolean.TRUE;
        }
    }

    public boolean setHasFooter(boolean hasFooter) {
        YUIUnit footer = getLayout().getFooter();
        if (footer != null
                && hasWebContentInUnit(((YUIAbstractComponent) footer).getId())) {
            eventBus.fireEvent(new SendMessageEvent(errors.zoneIsNotEmpty(),
                    Severity.ERROR));
            return Boolean.FALSE;
        } else {
            if (hasFooter) {
                updateLayoutFooterService(new YUIUnitImpl());
            } else {
                updateLayoutFooterService(null);
            }

            return Boolean.TRUE;
        }
    }

    public void createZone() {
        YUIComponentZone component = new YUIComponentZoneImpl(
                defaultNewZoneTemplate);

        for (int i = 0; i < component.getTemplate().getNumberOfComponents(); i++) {
            component.addComponent(new YUIUnitImpl());
        }

        createZoneService(component);
    }

    public boolean updateZoneTemplate(int zoneIndex, YUITemplate template) {
        YUIComponentZoneImpl component = ((YUIComponentZoneImpl) layout.getContent().getComponents().get(
                zoneIndex));
        if (!component.getTemplate().equals(template)) {
            int actualNumberOfUnits = component.getTemplate().getNumberOfComponents();
            int wantedNumberOfUnits = template.getNumberOfComponents();

            if (actualNumberOfUnits > wantedNumberOfUnits) {
                for (int i = actualNumberOfUnits - 1; i > wantedNumberOfUnits - 1; i--) {
                    YUIUnitImpl unit = (YUIUnitImpl) ((YUIComponent) component).getComponents().get(
                            i);
                    if (hasWebContentInUnit(unit.getId())) {
                        eventBus.fireEvent(new SendMessageEvent(
                                errors.unitIsNotEmpty(), Severity.ERROR));
                        return Boolean.FALSE;
                    }
                }
            }

            updateYUIZoneService(component, zoneIndex, template);
            return Boolean.TRUE;
        }
        return Boolean.TRUE;
    }

    public boolean deleteZone(int zoneIndex) {
        if (hasWebContentInZone(zoneIndex)) {
            eventBus.fireEvent(new SendMessageEvent(errors.zoneIsNotEmpty(),
                    Severity.ERROR));
            return Boolean.FALSE;
        } else {
            deleteZoneService(zoneIndex);

            return Boolean.TRUE;
        }
    }

    public void addWebContent(WebContentData webContent) {
        if (layout.getContent().getComponents().size() != 0) {
            String unitId = ((YUIUnitImpl) layout.getContent().getComponents().get(
                    0).getComponents().get(0)).getId();

            int position = 0;
            if (webContents.get(unitId) != null) {
                position = webContents.get(unitId).size();
            }

            webContent.setPosition(position);
            webContent.setUnitId(unitId);
            webContent.setName(webContent.getAssociatedType());

            createWebContentService(webContent);
        } else {
            eventBus.fireEvent(new SendMessageEvent(errors.noZoneCreated(),
                    Severity.ERROR));
        }
    }

    public void updateWebContent(String webContentId, List<String> list) {
        updateWebContentService(webContentId, list);
    }

    public void removeWebContent(String webContentId) {
        deleteWebContentService(webContentId);
    }

    public void webContentMoved(String fromUnitId, int fromWebContentPosition,
            String toUnitId, int toWebContentPosition) {
        if (!fromUnitId.equals(toUnitId)
                || fromWebContentPosition != toWebContentPosition) {
            WebContentData webContentToMove = webContents.get(fromUnitId).get(
                    fromWebContentPosition);

            webContents.get(fromUnitId).remove(fromWebContentPosition);
            if (!webContents.containsKey(toUnitId)) {
                webContents.put(toUnitId, new ArrayList<WebContentData>());
            }
            webContents.get(toUnitId).add(toWebContentPosition,
                    webContentToMove);

            webContentToMove.setUnitId(toUnitId);
            webContentToMove.setPosition(toWebContentPosition);

            updateAllWebContentsService();
        }
    }

    private void initApplicationService() {
        ContainerContext tempContainerContext = new ContainerContext(
                ContainerConfiguration.getSpaceId(),
                ContainerConfiguration.getRepositoryName(),
                ContainerConfiguration.getDocumentContextId(),
                ContainerConfiguration.getUserLanguage());
        tempContainerContext.setParameter("documentLinkBuilder",
                ContainerConfiguration.getDocumentLinkBuilder());
        tempContainerContext.setParameter("activityLinkBuilder",
                ContainerConfiguration.getActivityLinkBuilder());
        dispatcher.execute(new InitApplication(tempContainerContext,
                ContainerConfiguration.getSpaceProviderName(),
                ContainerConfiguration.getSpaceName()),
                new AbstractContainerAsyncCallback<InitApplicationResult>(
                        eventBus, errors.cannotLoadLayout()) {
                    @Override
                    public void doExecute(InitApplicationResult result) {
                        containerContext = new ContainerContext(
                                result.getSpaceId(),
                                ContainerConfiguration.getRepositoryName(),
                                ContainerConfiguration.getDocumentContextId(),
                                ContainerConfiguration.getUserLanguage());
                        containerContext.setParameter(
                                GENERATE_TITLE_PARAMETER_NAME,
                                Boolean.valueOf(
                                        ContainerConfiguration.generateTitle()).toString());
                        containerContext.setParameter("documentLinkBuilder",
                                ContainerConfiguration.getDocumentLinkBuilder());
                        containerContext.setParameter("activityLinkBuilder",
                                ContainerConfiguration.getActivityLinkBuilder());
                        setPermissions(result.getPermissions());
                        setLayout(result.getLayout());
                        eventBus.fireEvent(new LayoutLoadedEvent());
                        setWebContents(result.getWebContents());
                        eventBus.fireEvent(new WebContentsLoadedEvent());
                    }
                });
    }

    private void updateLayoutBodySizeService() {
        dispatcher.execute(
                new UpdateYUILayoutBodySize(containerContext,
                        getLayout().getBodySize()),
                new AbstractContainerAsyncCallback<UpdateYUILayoutBodySizeResult>(
                        eventBus, errors.cannotUpdateLayout()) {
                    @Override
                    public void doExecute(UpdateYUILayoutBodySizeResult result) {
                        eventBus.fireEvent(new ContainerSizeChangedEvent());
                    }
                });
    }

    private void createZoneService(final YUIComponentZone component) {
        dispatcher.execute(new CreateYUIZone(containerContext,
                (YUIComponentZone) component,
                getLayout().getContent().getComponents().size()),
                new AbstractContainerAsyncCallback<CreateYUIZoneResult>(
                        eventBus, errors.cannotCreateZone()) {
                    @Override
                    public void doExecute(CreateYUIZoneResult result) {
                        YUIComponentZone zone = result.getZone();

                        getLayout().getContent().addComponent(
                                (YUIComponent) zone);

                        for (int i = 0; i < zone.getTemplate().getNumberOfComponents(); i++) {
                            getWebContents().put(
                                    ((YUIComponent) zone).getComponents().get(i).getId(),
                                    new ArrayList<WebContentData>());
                        }

                        eventBus.fireEvent(new ZoneAddedEvent());
                    }
                });
    }

    private void updateYUIZoneService(final YUIComponentZone zone,
            final int zoneIndex, YUITemplate template) {
        dispatcher.execute(new UpdateYUIZone(containerContext, zone, zoneIndex,
                template),
                new AbstractContainerAsyncCallback<UpdateYUIZoneResult>(
                        eventBus, errors.cannotUpdateZone()) {
                    @Override
                    public void doExecute(UpdateYUIZoneResult result) {
                        for (YUIComponent unit : ((YUIComponent) result.getZone()).getComponents()) {
                            String idRef = ((YUIUnitImpl) unit).getId();

                            if (!getWebContents().containsKey(idRef)) {
                                getWebContents().put(idRef,
                                        new ArrayList<WebContentData>());
                            }

                            getLayout().getContent().getComponents().remove(
                                    zoneIndex);
                            getLayout().getContent().getComponents().add(
                                    zoneIndex, (YUIComponent) result.getZone());

                            eventBus.fireEvent(new ZoneUpdatedEvent(zoneIndex));
                        }
                    }
                });
    }

    private void updateLayoutSideBarService(final YUISideBarStyle sidebar) {
        dispatcher.execute(
                new UpdateYUILayoutSideBar(containerContext, sidebar),
                new AbstractContainerAsyncCallback<UpdateYUILayoutSideBarResult>(
                        eventBus, errors.cannotUpdateSideBar()) {
                    @Override
                    public void doExecute(UpdateYUILayoutSideBarResult result) {
                        if (sidebar.equals(YUISideBarStyle.YUI_SB_NO_COLUMN)) {
                            webContents.remove(((YUIComponent) layout.getSideBar()).getId());
                        } else {
                            if (!webContents.containsKey(((YUIComponent) result.getSideBar()).getId())) {
                                webContents.put(
                                        ((YUIComponent) result.getSideBar()).getId(),
                                        new ArrayList<WebContentData>());
                            }
                        }

                        layout.setSideBarStyle(sidebar);
                        layout.setSideBar(result.getSideBar());

                        eventBus.fireEvent(new SideBarChangedEvent());
                    }
                });
    }

    private void updateLayoutHeaderService(final YUIUnit header) {
        dispatcher.execute(
                new UpdateYUILayoutHeader(containerContext, header),
                new AbstractContainerAsyncCallback<UpdateYUILayoutHeaderResult>(
                        eventBus, errors.cannotUpdateHeader()) {
                    @Override
                    public void doExecute(UpdateYUILayoutHeaderResult result) {
                        if (result.getHeader() != null) {
                            String headerId = ((YUIComponent) result.getHeader()).getId();
                            webContents.put(headerId,
                                    new ArrayList<WebContentData>());
                        } else {
                            String headerId = ((YUIComponent) layout.getHeader()).getId();
                            webContents.remove(headerId);
                        }

                        layout.setHeader(result.getHeader());

                        eventBus.fireEvent(new HeaderSelectedEvent());
                    }
                });
    }

    private void updateLayoutFooterService(final YUIUnit footer) {
        dispatcher.execute(
                new UpdateYUILayoutFooter(containerContext, footer),
                new AbstractContainerAsyncCallback<UpdateYUILayoutFooterResult>(
                        eventBus, errors.cannotUpdateFooter()) {
                    @Override
                    public void doExecute(UpdateYUILayoutFooterResult result) {
                        if (result.getFooter() != null) {
                            String footerId = ((YUIComponent) result.getFooter()).getId();
                            webContents.put(footerId,
                                    new ArrayList<WebContentData>());
                        } else {
                            String footerId = ((YUIComponent) layout.getFooter()).getId();
                            webContents.remove(footerId);
                        }

                        layout.setFooter(result.getFooter());

                        eventBus.fireEvent(new FooterSelectedEvent());
                    }
                });
    }

    private void deleteZoneService(final int zoneIndex) {
        dispatcher.execute(new DeleteYUIZone(containerContext, zoneIndex),
                new AbstractContainerAsyncCallback<DeleteYUIZoneResult>(
                        eventBus, errors.cannotDeleteZone()) {
                    @Override
                    public void doExecute(DeleteYUIZoneResult result) {
                        for (YUIComponent unit : getLayout().getContent().getComponents().get(
                                zoneIndex).getComponents()) {
                            getWebContents().remove(
                                    ((YUIUnitImpl) unit).getId());
                        }

                        getLayout().getContent().getComponents().remove(
                                zoneIndex);

                        eventBus.fireEvent(new ZoneDeletedEvent(zoneIndex));
                    }
                });
    }

    private void createWebContentService(WebContentData data) {
        dispatcher.execute(new CreateWebContent(containerContext, data),
                new AbstractContainerAsyncCallback<CreateWebContentResult>(
                        eventBus, errors.cannotCreateWebContent()) {
                    @Override
                    public void doExecute(CreateWebContentResult result) {
                        WebContentData data = result.getData();
                        if (!getWebContents().containsKey(data.getUnitId())) {
                            webContents.put(data.getUnitId(),
                                    new ArrayList<WebContentData>());
                        }

                        webContents.get(data.getUnitId()).add(
                                (int) data.getPosition(), data);

                        permissions.put(data.getId(), result.getPermissions());

                        eventBus.fireEvent(new WebContentAddedEvent(
                                result.getData()));
                    }
                });
    }

    private void updateAllWebContentsService() {
        dispatcher.execute(new UpdateAllWebContents(containerContext,
                getWebContents()),
                new AbstractContainerAsyncCallback<UpdateAllWebContentsResult>(
                        eventBus, errors.cannotUpdateAllWebContents()) {
                    @Override
                    public void doExecute(UpdateAllWebContentsResult result) {
                        // Nothing to do...
                    }
                });
    }

    private void updateWebContentService(final String webContentId,
            List<String> list) {
        dispatcher.execute(new UpdateWebContent(containerContext,
                getWebContent(webContentId), list),
                new AbstractContainerAsyncCallback<UpdateWebContentResult>(
                        eventBus, errors.cannotUpdateWebContent()) {
                    @Override
                    public void doExecute(UpdateWebContentResult result) {
                        getWebContent(webContentId).updateFrom(
                                result.getWebContentData());
                        eventBus.fireEvent(new WebContentUpdatedEvent(
                                webContentId));
                    }
                });
    }

    private void deleteWebContentService(final String webContentId) {
        dispatcher.execute(new DeleteWebContent(containerContext,
                getWebContent(webContentId)),
                new AbstractContainerAsyncCallback<DeleteWebContentResult>(
                        eventBus, errors.cannotDeleteWebContent()) {
                    @Override
                    public void doExecute(DeleteWebContentResult result) {
                        WebContentData data = getWebContent(webContentId);
                        getWebContents().get(data.getUnitId()).remove(data);
                        eventBus.fireEvent(new WebContentRemovedEvent(
                                webContentId));
                    }
                });
    }

}
