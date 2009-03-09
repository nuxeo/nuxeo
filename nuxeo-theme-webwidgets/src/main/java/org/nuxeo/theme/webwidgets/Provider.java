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

import java.util.List;
import java.util.Map;

public interface Provider {

    /**
     * Create a new widget. The implementation is responsible for generating a
     * unique widget identifier.
     *
     * @param widgetTypeName the widget type name
     * @return the created widget
     */
    Widget createWidget(String widgetTypeName);

    /**
     * Get a widget by its id.
     *
     * @param uid the widget's unique identifier
     * @return the widget or null if the identifier does not match any widget
     */
    Widget getWidgetByUid(String uid);

    /**
     * Get the list of widgets in a region.
     *
     * @param regionName the name of the region from which to get the list of
     *            widgets
     * @return an ordered list of widgets located in the specified region
     */
    List<Widget> getWidgets(String regionName);

    /**
     * Add a widget to a region.
     *
     * @param widget the widget to add
     * @param regionName the name of the region
     * @param order the order at which to insert the widget (begins with 0)
     */
    void addWidget(Widget widget, String regionName, int order);

    /**
     * Move a widget to another region.
     *
     * @param widget the widget to move
     * @param destRegionName the name of the destination region
     * @param order the order at which to insert the widget
     */
    void moveWidget(Widget widget, String destRegionName, int order);

    /**
     * Reorder a widget.
     *
     * @param widget the widget to reorder
     * @param order the new order
     */
    void reorderWidget(Widget widget, int order);

    /**
     * Remove a widget. The implementation is responsible for removing from the
     * widget from the region in which it is located and for destroying the
     * widget.
     *
     * @param widget the widget to remove
     */
    void removeWidget(Widget widget);

    /**
     * Get the region of a widget.
     *
     * @param widget the widget to get the region of
     * @return the name of the region or null if the widget does not exist.
     */
    String getRegionOfWidget(Widget widget);

    /**
     * Get the preferences of a widget.
     *
     * @param widget the widget whose preferences are to be obtained
     * @return a mapping of preferences as <preference name, preference value>
     */
    Map<String, String> getWidgetPreferences(Widget widget);

    /**
     * Set the preferences of a widget. Existing preferences are replaced.
     *
     * @param widget the widget whose preferences will be set
     * @param preferences a mapping of preferences as <preference name,
     *            preference value>
     */
    void setWidgetPreferences(Widget widget, Map<String, String> preferences);

    /**
     * Set the state of a widget.
     *
     * @param widget the widget whose state is to be set
     * @param state the state to set (see ${@link WidgetState})
     */
    void setWidgetState(Widget widget, WidgetState state);

    /**
     * Get the state of a widget.
     *
     * @param widget the widget whose state is to be obtained
     * @return the state (see ${@link WidgetState})
     */
    WidgetState getWidgetState(Widget widget);

    /**
     * Get the data of a widget
     *
     * @param widget the widget from which to get the data
     * @param dataName the name of the data
     * @return
     */
    WidgetData getWidgetData(Widget widget, String dataName);

    /**
     * Set data to a widget
     *
     * @param widget the widget to set data to
     * @param dataName the name of the data
     * @param data the widget data
     */
    void setWidgetData(Widget widget, String dataName, WidgetData data);

    /**
     * Delete all data associated with a widget
     *
     * @param widget the widget
     */
    void deleteWidgetData(Widget widget);

    /**
     * Check for read access.
     *
     * @return true if the current principal can read information from this
     *         provider.
     */
    boolean canRead();

    /**
     * Check write access.
     *
     * @return true if the current principal can write information to this
     *         provider.
     */
    boolean canWrite();

}
