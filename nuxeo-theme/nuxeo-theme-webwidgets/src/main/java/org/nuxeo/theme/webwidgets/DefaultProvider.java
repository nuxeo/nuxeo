/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.session.UserSession;

public class DefaultProvider implements Provider {

    private static final Log log = LogFactory.getLog(DefaultProvider.class);

    private static final String PROVIDER_SESSION_ID = "org.nuxeo.theme.webwidgets.default_provider_session";

    public DefaultProvider() {
    }

    public DefaultProviderSession getDefaultProviderSession() {
        UserSession userSession = WebEngine.getActiveContext().getUserSession();
        DefaultProviderSession session = (DefaultProviderSession) userSession.get(PROVIDER_SESSION_ID);
        if (session == null) {
            session = new DefaultProviderSession();
            userSession.put(PROVIDER_SESSION_ID, session);
        }
        return session;
    }

    public Widget createWidget(String widgetTypeName) {
        DefaultProviderSession session = getDefaultProviderSession();
        int counter = session.getCounter();
        final String uid = Integer.toString(counter);
        counter++;
        session.setCounter(counter);
        final Widget widget = new DefaultWidget(widgetTypeName, uid);
        session.getWidgetsByUid().put(uid, widget);
        log.debug("Created web widget '" + widgetTypeName + "' (uid " + uid
                + ")");
        return widget;
    }

    public Widget getWidgetByUid(String uid) {
        DefaultProviderSession session = getDefaultProviderSession();
        return session.getWidgetsByUid().get(uid);
    }

    public List<Widget> getWidgets(String regionName) {
        DefaultProviderSession session = getDefaultProviderSession();
        return session.getWidgetsByRegion().get(regionName);
    }

    public void addWidget(Widget widget, String regionName, int order) {
        DefaultProviderSession session = getDefaultProviderSession();
        Map<String, List<Widget>> widgetsByRegion = session.getWidgetsByRegion();
        if (!widgetsByRegion.containsKey(regionName)) {
            widgetsByRegion.put(regionName, new ArrayList<Widget>());
        }
        widgetsByRegion.get(regionName).add(order, widget);
        session.getRegionsByUid().put(widget.getUid(), regionName);
        log.debug("Added web widget '" + widget.getName() + "' (uid "
                + widget.getUid() + ") into region '" + regionName
                + "' at position " + order);
    }

    public void moveWidget(Widget widget, String destRegionName, int order) {
        DefaultProviderSession session = getDefaultProviderSession();
        final String srcRegionName = getRegionOfWidget(widget);
        Map<String, List<Widget>> widgetsByRegion = session.getWidgetsByRegion();
        widgetsByRegion.get(srcRegionName).remove(widget);
        if (!widgetsByRegion.containsKey(destRegionName)) {
            widgetsByRegion.put(destRegionName, new ArrayList<Widget>());
        }
        widgetsByRegion.get(destRegionName).add(order, widget);
        session.getRegionsByUid().put(widget.getUid(), destRegionName);
        log.debug("Moved web widget '" + widget.getName() + "' (uid "
                + widget.getUid() + ") from region '" + srcRegionName
                + "' to '" + destRegionName + "' at position " + order);
    }

    public void reorderWidget(Widget widget, int order) {
        DefaultProviderSession session = getDefaultProviderSession();
        final String regionName = getRegionOfWidget(widget);
        Map<String, List<Widget>> widgetsByRegion = session.getWidgetsByRegion();
        final List<Widget> widgets = widgetsByRegion.get(regionName);
        final int oldOrder = widgets.indexOf(widget);
        widgets.remove(oldOrder);
        widgets.add(order, widget);
        log.debug("Reordered web widget '" + widget.getName() + "' (uid "
                + widget.getUid() + ") in region '" + regionName
                + "' to position " + order);
    }

    public void removeWidget(Widget widget) {
        DefaultProviderSession session = getDefaultProviderSession();
        final String uid = widget.getUid();
        final String regionName = getRegionOfWidget(widget);
        Map<String, List<Widget>> widgetsByRegion = session.getWidgetsByRegion();
        widgetsByRegion.get(regionName).remove(widget);
        session.getWidgetsByUid().remove(uid);
        log.debug("Removed web widget '" + widget.getName() + "' (uid " + uid
                + ") from region '" + regionName + "'");
    }

    public String getRegionOfWidget(Widget widget) {
        DefaultProviderSession session = getDefaultProviderSession();
        return session.getRegionsByUid().get(widget.getUid());
    }

    public Map<String, String> getWidgetPreferences(Widget widget) {
        DefaultProviderSession session = getDefaultProviderSession();
        return session.getPreferencesByWidget().get(widget);
    }

    public void setWidgetPreferences(Widget widget,
            Map<String, String> preferences) {
        DefaultProviderSession session = getDefaultProviderSession();
        session.getPreferencesByWidget().put(widget, preferences);
    }

    public void setWidgetState(Widget widget, WidgetState state) {
        DefaultProviderSession session = getDefaultProviderSession();
        session.getStatesByWidget().put(widget, state);
    }

    public WidgetState getWidgetState(Widget widget) {
        DefaultProviderSession session = getDefaultProviderSession();
        return session.getStatesByWidget().get(widget);
    }

    public WidgetData getWidgetData(Widget widget, String dataName) {
        DefaultProviderSession session = getDefaultProviderSession();
        Map<Widget, Map<String, WidgetData>> dataByWidget = session.getDataByWidget();
        if (dataByWidget.containsKey(widget)) {
            return dataByWidget.get(widget).get(dataName);
        }
        return null;
    }

    public void setWidgetData(Widget widget, String dataName, WidgetData data) {
        DefaultProviderSession session = getDefaultProviderSession();
        Map<Widget, Map<String, WidgetData>> dataByWidget = session.getDataByWidget();
        if (!dataByWidget.containsKey(widget)) {
            dataByWidget.put(widget, new HashMap<String, WidgetData>());
        }
        dataByWidget.get(widget).put(dataName, data);
    }

    public void deleteWidgetData(Widget widget) {
        DefaultProviderSession session = getDefaultProviderSession();
        Map<Widget, Map<String, WidgetData>> dataByWidget = session.getDataByWidget();
        if (dataByWidget.containsKey(widget)) {
            dataByWidget.remove(widget);
        }
    }

    /*
     * Security
     */
    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return true;
    }

    public void destroy() {
        // TODO Auto-generated method stub

    }

}
