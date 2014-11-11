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

package org.nuxeo.theme.webwidgets.providers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.theme.webwidgets.Widget;
import org.nuxeo.theme.webwidgets.WidgetData;
import org.nuxeo.theme.webwidgets.WidgetState;

public class DefaultProviderSession {

    private final Map<String, List<Widget>> widgetsByRegion;

    private final Map<String, Widget> widgetsByUid;

    private final Map<String, String> regionsByUid;

    private final Map<Widget, Map<String, String>> preferencesByWidget;

    private final Map<Widget, Map<String, WidgetData>> dataByWidget;

    private final Map<Widget, WidgetState> statesByWidget;

    private int counter = 0;

    public DefaultProviderSession() {
        widgetsByRegion = new HashMap<String, List<Widget>>();
        widgetsByUid = new HashMap<String, Widget>();
        regionsByUid = new HashMap<String, String>();
        preferencesByWidget = new HashMap<Widget, Map<String, String>>();
        dataByWidget = new HashMap<Widget, Map<String, WidgetData>>();
        statesByWidget = new HashMap<Widget, WidgetState>();
        counter = 0;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public Map<String, List<Widget>> getWidgetsByRegion() {
        return widgetsByRegion;
    }

    public Map<String, Widget> getWidgetsByUid() {
        return widgetsByUid;
    }

    public Map<String, String> getRegionsByUid() {
        return regionsByUid;
    }

    public Map<Widget, Map<String, String>> getPreferencesByWidget() {
        return preferencesByWidget;
    }

    public Map<Widget, Map<String, WidgetData>> getDataByWidget() {
        return dataByWidget;
    }

    public Map<Widget, WidgetState> getStatesByWidget() {
        return statesByWidget;
    }

    public Widget getWidgetByUid(String uid) {
        return widgetsByUid.get(uid);
    }

    public List<Widget> getWidgets(String regionName) {
        return widgetsByRegion.get(regionName);
    }

    public void clear() {
        widgetsByRegion.clear();
        widgetsByUid.clear();
        regionsByUid.clear();
        preferencesByWidget.clear();
        dataByWidget.clear();
        statesByWidget.clear();
        counter = 0;
    }

}
