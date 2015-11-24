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
import java.util.Map;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.opensocial.container.client.AppErrorMessages;
import org.nuxeo.opensocial.container.client.AppInfoMessages;
import org.nuxeo.opensocial.container.client.Container;
import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.bundles.ImagesBundle;
import org.nuxeo.opensocial.container.client.event.priv.app.HideMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.service.LayoutLoadedEvent;
import org.nuxeo.opensocial.container.client.event.priv.service.LayoutLoadedEventHandler;
import org.nuxeo.opensocial.container.client.event.publ.UpdateWebContentEvent;
import org.nuxeo.opensocial.container.client.event.publ.UpdateWebContentEventHandler;
import org.nuxeo.opensocial.container.client.gin.ClientInjector;
import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.client.utils.JSParams;
import org.nuxeo.opensocial.container.client.utils.Severity;
import org.nuxeo.opensocial.container.client.view.PortletWidget;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.UserPref;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Frame;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Stéphane Fourrier
 */
public class AppPresenter extends WidgetPresenter<AppPresenter.Display> {
    private final ClientInjector injector = Container.injector;

    public static AppErrorMessages errors = GWT.create(AppErrorMessages.class);

    public static AppInfoMessages infos = GWT.create(AppInfoMessages.class);

    public static ContainerConstants containerConstants = GWT.create(ContainerConstants.class);

    public static ImagesBundle images = GWT.create(ImagesBundle.class);

    public interface Display extends WidgetDisplay {
        void addContent(WidgetDisplay display);
    }

    public static final Place PLACE = new Place("App");

    private ContainerPresenter containerPresenter;

    private Provider<ContainerBuilderPresenter> containerBuilderPresenter;

    private boolean containerBuilderBinded = false;

    private AppModel model;

    @Inject
    public AppPresenter(Display display, EventBus eventBus,
            ContainerPresenter containerPresenter,
            Provider<ContainerBuilderPresenter> containerBuilderPresenter,
            AppModel model) {
        super(display, eventBus);

        this.containerPresenter = containerPresenter;
        this.containerBuilderPresenter = containerBuilderPresenter;
        this.model = model;
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        eventBus.fireEvent(new SendMessageEvent(infos.isLoading(),
                Severity.SUCCESS, true));

        containerPresenter.bind();

        registerLayoutLoadEvent();
        registerExternalPortletUpdateEvent();
        registerExternalCall(this);
    }

    private void registerExternalPortletUpdateEvent() {
        registerHandler(eventBus.addHandler(UpdateWebContentEvent.TYPE,
                new UpdateWebContentEventHandler() {
                    public void onUpdateWebContent(UpdateWebContentEvent event) {
                        model.updateWebContent(event.getWebContentId(),
                                event.getFiles());
                    }
                }));
    }

    @Override
    protected void onUnbind() {
        if (containerPresenter != null) {
            containerPresenter.unbind();
        }
        if (containerBuilderPresenter != null) {
            containerBuilderPresenter.get().unbind();
        }
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
        containerPresenter.revealDisplay();
        display.addContent(containerPresenter.getDisplay());
    }

    private void registerLayoutLoadEvent() {
        registerHandler(eventBus.addHandler(LayoutLoadedEvent.TYPE,
                new LayoutLoadedEventHandler() {
                    public void onLayoutLoaded(LayoutLoadedEvent event) {
                        containerPresenter.refreshDisplay();
                    }
                }));
    }

    private native void registerExternalCall(AppPresenter me) /*-{
        var container = $wnd.nuxeo.openSocial.container;

        container.addGadget = function(type, params) {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::addGadget(Ljava/lang/String;Lorg/nuxeo/opensocial/container/client/utils/JSParams;)(type, params);
        }

        container.openContainerBuilder = function() {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::openContainerBuilder()();
        }

        // this.f represents the "name" attribute of the frame, not the "id"
        var rpc = $wnd.gadgets.rpc;

        rpc.register('resize_iframe', function(height) {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::resizeOpenSocialWebContent(Ljava/lang/String;I)(this.f, height);
        });

        rpc.register('refresh', function() {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::refreshOpenSocialWebContent(Ljava/lang/String;)(this.f);
        });

        rpc.register('set_pref', function(editToken, name, value) {
        for ( var i = 1, j = arguments.length; i < j; i += 2) {
        if(arguments[i]!="refresh") {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::setOpenSocialUserPref(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(this.f,arguments[i],arguments[i+1]);
        }
        }
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::saveOpenSocialUserPref(Ljava/lang/String;)(this.f);
        });

        rpc.register('set_title', function(title) {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::setOpenSocialWebContentTitle(Ljava/lang/String;Ljava/lang/String;)(this.f, title);
        });

        rpc.register('show_fancybox', function(childs, current) {
        if($wnd.jQuery.fancybox) {
        var items = [];

        $wnd.jQuery.each(childs, function(index, child) {
        items.push({href:[child.path.value,"@view/Original.jpg"].join(""), title:child.description.value});
        });

        $wnd.jQuery.fancybox(items, {
        'titleShow' : true,
        'titlePosition' : 'inside',
        'zoomSpeedIn': 500,
        'zoomSpeedOut': 500,
        'overlayShow': false,
        'forceImage': true,
        'hideOnContentClick': false
        }, current);
        } else if($wnd.console) {
        $wnd.console.error("Add FancyBox plugin v1.3.0");
        }
        });

        rpc.register('get_nuxeo_space_id', function() {
        me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::getNuxeoSpaceId()();
        });

        if (typeof $wnd.gadgets.pubsubrouter !== 'undefined') {
          $wnd.gadgets.pubsubrouter.init(
              function(id) {
                  return me.@org.nuxeo.opensocial.container.client.presenter.AppPresenter::getGadgetUrlByGadgetId(Ljava/lang/String;)(id);
              }
          );
        }

    }-*/;

    @SuppressWarnings("unused")
    private String getNuxeoSpaceId() {
        return model.getContainerContext().getSpaceId();
    }

    @SuppressWarnings("unused")
    private void setOpenSocialWebContentTitle(String frameId, String title) {
        String webContentId = getWebContentId(frameId);

        try {
            model.getWebContent(webContentId).setTitle(title);
            ((PortletWidget) containerPresenter.getDisplay().getWebContent(
                    webContentId)).setTitle(title);
        } catch (ClassCastException e) {
            // TODO The widget is not a PortletWidget
        }

        model.updateWebContent(webContentId, null);
    }

    private String getGadgetUrlByGadgetId(String frameId) {
        try {
            return ((OpenSocialData) model.getWebContent(getWebContentId(frameId))).getGadgetDef();
        } catch (ClassCastException e) {
            eventBus.fireEvent(new SendMessageEvent(
                    errors.cannotFindWebContent(), Severity.ERROR));
            return null;
        }
    }

    @SuppressWarnings("unused")
    private void setOpenSocialUserPref(String frameId, String name, String value) {
        try {
            UserPref userPref = ((OpenSocialData) model.getWebContent(getWebContentId(frameId))).getUserPrefByName(name);
            if (userPref != null) {
                userPref.setActualValue(value);
            } else {
                eventBus.fireEvent(new SendMessageEvent(
                        errors.preferenceDoesNotExist(name), Severity.ERROR));
            }
        } catch (ClassCastException e) {
            eventBus.fireEvent(new SendMessageEvent(
                    errors.cannotFindWebContent(), Severity.ERROR));
        }
    }

    @SuppressWarnings("unused")
    private void saveOpenSocialUserPref(String frameId) {
        model.updateWebContent(getWebContentId(frameId), null);
    }

    @SuppressWarnings("unused")
    private void resizeOpenSocialWebContent(String frameId, int height) {
        Element frame = getOpenSocialFrameById(frameId);
        if (frame != null) {
            Frame uiFrame = Frame.wrap(frame);
            uiFrame.setHeight(height + "px");
        }
    }

    @SuppressWarnings("unused")
    private void refreshOpenSocialWebContent(String frameId) {
        Element frame = getOpenSocialFrameById(frameId);
        if (frame != null) {
            Frame uiFrame = Frame.wrap(frame);
            uiFrame.setUrl(uiFrame.getUrl());
        }
    }

    private Element getOpenSocialFrameById(String frameId) {
        return DOM.getElementById(frameId);
    }

    // TODO this method has to be improved ! The frame is prefixed by
    // "open-social-" and
    // has to be changed later on
    private String getWebContentId(String frameId) {
        return frameId.substring(12);
    }

    @SuppressWarnings("unused")
    private void addGadget(String type, JSParams<?> params) {
        Map<String, String> prefs;
        WebContentData webContent = injector.getGadgetFactory().getDataFor(type);
        if (params != null) {
            prefs = params.toMap();
        } else {
            prefs = new HashMap<String, String>();
        }

        if (webContent != null && webContent.initPrefs(prefs) == true) {
            model.addWebContent(webContent);
        } else {
            eventBus.fireEvent(new SendMessageEvent(
                    errors.cannotAddExternalWebContent(type), Severity.ERROR));
        }
    }

    @SuppressWarnings("unused")
    private void openContainerBuilder() {
        eventBus.fireEvent(new SendMessageEvent(infos.isLoading(),
                Severity.INFO, true));
        if (containerBuilderBinded == false) {
            GWT.runAsync(new RunAsyncCallback() {
                public void onSuccess() {
                    containerBuilderPresenter.get().bind();
                    containerBuilderPresenter.get().revealDisplay();
                    containerBuilderBinded = true;
                    eventBus.fireEvent(new HideMessageEvent());
                }

                public void onFailure(Throwable reason) {
                    eventBus.fireEvent(new SendMessageEvent(
                            errors.cannotLoadContainerBuilder(), Severity.ERROR));
                }
            });
        } else {
            containerBuilderPresenter.get().revealDisplay();
            eventBus.fireEvent(new HideMessageEvent());
        }
    }
}
