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

import static org.jboss.seam.ScopeType.SESSION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ejb.SeamInterceptor;

@Stateful
@Name("org.nuxeo.theme.webwidgets.DefaultProvider")
@Scope(SESSION)
@Interceptors(SeamInterceptor.class)
public class DefaultProvider implements DefaultProviderLocal {

    private static final Log log = LogFactory.getLog(DefaultProvider.class);

    private final Map<String, List<Widget>> widgetsByRegion = new HashMap<String, List<Widget>>();

    private final Map<String, Widget> widgetsByUid = new HashMap<String, Widget>();

    private final Map<String, String> regionsByUid = new HashMap<String, String>();

    private final Map<Widget, Map<String, String>> preferencesByWidget = new HashMap<Widget, Map<String, String>>();

    private final Map<Widget, Map<String, WidgetData>> dataByWidget = new HashMap<Widget, Map<String, WidgetData>>();

    private final Map<Widget, WidgetState> statesByWidget = new HashMap<Widget, WidgetState>();

    private int counter = 0;

    public Widget createWidget(String widgetTypeName) {
        final String uid = Integer.toString(counter);
        counter++;
        final Widget widget = new DefaultWidget(widgetTypeName, uid);
        widgetsByUid.put(uid, widget);
        log.debug("Created web widget '" + widgetTypeName + "' (uid " + uid
                + ")");
        return widget;
    }

    public Widget getWidgetByUid(String uid) {
        return widgetsByUid.get(uid);
    }

    public List<Widget> getWidgets(String regionName) {
        return widgetsByRegion.get(regionName);
    }

    public void addWidget(Widget widget, String regionName, int order) {
        if (!widgetsByRegion.containsKey(regionName)) {
            widgetsByRegion.put(regionName, new ArrayList<Widget>());
        }
        widgetsByRegion.get(regionName).add(order, widget);
        regionsByUid.put(widget.getUid(), regionName);
        log.debug("Added web widget '" + widget.getName() + "' (uid "
                + widget.getUid() + ") into region '" + regionName
                + "' at position " + order);
    }

    public void moveWidget(Widget widget, String destRegionName, int order) {
        final String srcRegionName = getRegionOfWidget(widget);
        widgetsByRegion.get(srcRegionName).remove(widget);
        if (!widgetsByRegion.containsKey(destRegionName)) {
            widgetsByRegion.put(destRegionName, new ArrayList<Widget>());
        }
        widgetsByRegion.get(destRegionName).add(order, widget);
        regionsByUid.put(widget.getUid(), destRegionName);
        log.debug("Moved web widget '" + widget.getName() + "' (uid "
                + widget.getUid() + ") from region '" + srcRegionName
                + "' to '" + destRegionName + "' at position " + order);
    }

    public void reorderWidget(Widget widget, int order) {
        final String regionName = getRegionOfWidget(widget);
        final List<Widget> widgets = widgetsByRegion.get(regionName);
        final int oldOrder = widgets.indexOf(widget);
        widgets.remove(oldOrder);
        widgets.add(order, widget);
        log.debug("Reordered web widget '" + widget.getName() + "' (uid "
                + widget.getUid() + ") in region '" + regionName
                + "' to position " + order);
    }

    public void removeWidget(Widget widget) {
        final String uid = widget.getUid();
        final String regionName = getRegionOfWidget(widget);
        widgetsByRegion.get(regionName).remove(widget);
        widgetsByUid.remove(uid);
        log.debug("Removed web widget '" + widget.getName() + "' (uid " + uid
                + ") from region '" + regionName + "'");
    }

    public String getRegionOfWidget(Widget widget) {
        return regionsByUid.get(widget.getUid());
    }

    public Map<String, String> getWidgetPreferences(Widget widget) {
        return preferencesByWidget.get(widget);
    }

    public void setWidgetPreferences(Widget widget, Map<String, String> preferences) {
        preferencesByWidget.put(widget, preferences);
    }

    public void setWidgetState(Widget widget, WidgetState state) {
        statesByWidget.put(widget, state);
    }

    public WidgetState getWidgetState(Widget widget) {
        return statesByWidget.get(widget);
    }

    public WidgetData getWidgetData(Widget widget, String dataName) {
        if (dataByWidget.containsKey(widget)) {
            return dataByWidget.get(widget).get(dataName);
        }
        return null;
    }

    public void setWidgetData(Widget widget, String dataName, WidgetData data) {
        if (!dataByWidget.containsKey(widget)) {
            dataByWidget.put(widget, new HashMap<String, WidgetData>());
        }
        dataByWidget.get(widget).put(dataName, data);
    }

    public void deleteWidgetData(Widget widget) {
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

    @Remove
    @Destroy
    public void destroy() {
        log.debug("Removed SEAM component: org.nuxeo.theme.webwidgets.DefaultProvider");
    }

}
