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

package org.nuxeo.opensocial.container.client.external.opensocial;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.opensocial.container.client.ContainerConfiguration;
import org.nuxeo.opensocial.container.client.ui.api.HasId;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;

import com.google.gwt.i18n.client.LocaleInfo;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialPresenter extends
        WidgetPresenter<OpenSocialPresenter.Display> {

    public static final String OS_LANG_ATTRIBUTE = "lang";

    public static final String OS_VIEW_ATTRIBUTE = "view";

    public static final String OS_PERMISSIONS_ATTRIBUTE = "permission";

    public static final String OS_PARENT_ATTRIBUTE = "parent";
    
    public interface Display extends WidgetDisplay, HasId {
        void setUrl(String url);

        void setHeight(int height);

        void setName(String name);

        void enableFacets();
    }

    public static final Place PLACE = null;

    private OpenSocialModel model;

    public OpenSocialPresenter(Display display, EventBus eventBus,
            OpenSocialModel model) {
        super(display, eventBus);

        this.model = model;
        fetchContent();
    }

    private void fetchContent() {
        display.setId("open-social-" + model.getData().getId());
        display.setName("open-social-" + model.getData().getId());
        display.asWidget().getElement().setAttribute("frameBorder", "0");
        display.asWidget().getElement().setAttribute("scrolling", "no");
        display.asWidget().addStyleName(model.getData().getGadgetName());

        setLanguage();
        setPermissions();
        setParent();
        setHeight();

        display.setUrl(model.getData().getFrameUrl());

        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            display.enableFacets();
        }
    }

    public void setLanguage() {
        String locale = LocaleInfo.getCurrentLocale().getLocaleName();
        String userLanguage = ContainerConfiguration.getUserLanguage();
        if (userLanguage != null && !userLanguage.isEmpty()) {
            locale = userLanguage;
        }
        model.getData().setFrameUrl(
                changeParam(model.getData().getFrameUrl(), OS_LANG_ATTRIBUTE,
                        locale));
    }

    public void setPermissions() {
        Map<String, Boolean> permissions = model.getPermissions();
        StringBuilder permissionsStr = new StringBuilder();
        for (Entry<String, Boolean> entry : permissions.entrySet()) {
            if (entry.getValue()) {
                permissionsStr.append(entry.getKey()).append(",");
            }
        }
        if (!permissions.isEmpty()) {
            permissionsStr.deleteCharAt(permissionsStr.length() - 1);
        }
        model.getData().setFrameUrl(
                changeParam(model.getData().getFrameUrl(),
                        OS_PERMISSIONS_ATTRIBUTE, 
                        "[" + permissionsStr.toString() + "]")); 
    }

    public void setView(String view) {
        model.getData().setFrameUrl(
                changeParam(model.getData().getFrameUrl(), OS_VIEW_ATTRIBUTE,
                        view));
    }

    public void setParent() {
        model.getData().setFrameUrl(
                changeParam(model.getData().getFrameUrl(), OS_PARENT_ATTRIBUTE,
                        ContainerConfiguration.getBaseUrl()));
    }

    // Make this method static in order to be easily tested !
    public static String changeParam(String url, String name, String value) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(name, value);
        url = URIUtils.addParametersToURIQuery(url, parameters);
        return url;
    }

    protected void setHeight() {
        String heightPref = model.getData().getModulePreferences().get("height");
        if (heightPref != null) {
            try {
                int height = Integer.parseInt(heightPref);
                display.setHeight(height);
            } catch(NumberFormatException e) {
                // do nothing
            }
        }
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
        setLanguage();
        setPermissions();
        setParent();
        setHeight();
        display.setUrl(model.getData().getFrameUrl());
    }

    public void revealDisplay() {
    }
}
