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

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.remoting.WebRemote;

@Local
public interface ManagerLocal {

    List<WidgetType> getAvailableWidgetTypes();

    List<SelectItem> getAvailableWidgetCategories();

    String getWidgetCategory();

    @WebRemote
    void setWidgetCategory(String widgetCategory);

    @WebRemote
    void addWidget(String providerName, String widgetTypeName, String region,
            int order);

    @WebRemote
    String moveWidget(String srcProviderName, String destProviderName,
            String srcUid, String srcRegionName, String destRegionName,
            int destOrder);

    @WebRemote
    void removeWidget(String providerName, String uid);

    @WebRemote
    void updateWidgetPreferences(String providerName, String uid,
            Map<String, String> preferences);

    @WebRemote
    void setWidgetState(String providerName, String uid, String stateName);

    @WebRemote
    String getPanelData(String providerName, String regionName, String mode);

    @WebRemote
    String getWidgetDataInfo(String providerName, String uid, String dataName);

    WidgetData getWidgetData(String providerName, String uid, String dataName);

    void setWidgetData(String providerName, String uid, String dataName,
            WidgetData data);

    @Remove
    void destroy();

}
