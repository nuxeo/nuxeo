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

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.webwidgets.Provider;
import org.nuxeo.theme.webwidgets.Widget;
import org.nuxeo.theme.webwidgets.WidgetData;
import org.nuxeo.theme.webwidgets.WidgetState;

public class PersistentProvider implements Provider {

    private static final Log log = LogFactory.getLog(PersistentProvider.class);

    protected EntityManager em;

    protected EntityTransaction et;

    protected Principal currentNuxeoPrincipal;

    public PersistentProvider() {
        WebContext ctx = WebEngine.getActiveContext();
        em = PersistenceConfigurator.getEntityManager();
        if (ctx != null) {
            currentNuxeoPrincipal = ctx.getPrincipal();
        }
    }

    public void addWidget(Widget widget, String regionName, int order) {
        List<Widget> widgets = new ArrayList<Widget>(getWidgets(regionName));
        ((WidgetEntity) widget).setRegion(regionName);
        widgets.add(order, widget);
        reorderWidgets(widgets);
    }

    synchronized public Widget createWidget(String widgetTypeName) {
        final WidgetEntity widget = new WidgetEntity(widgetTypeName);
        begin();
        em.persist(widget);
        commit();
        return widget;
    }

    public String getRegionOfWidget(Widget widget) {
        return ((WidgetEntity) widget).getRegion();
    }

    public WidgetState getWidgetState(Widget widget) {
        return ((WidgetEntity) widget).getState();
    }

    synchronized public Widget getWidgetByUid(String uid) {
        return (Widget) em.find(WidgetEntity.class, Integer.valueOf(uid));
    }

    @SuppressWarnings("unchecked")
    synchronized public List<Widget> getWidgets(String regionName) {
        List<Widget> widgets = new ArrayList<Widget>();
        widgets.addAll(em.createNamedQuery("Widget.findAll").setParameter(
                "region", regionName).getResultList());
        return widgets;
    }

    public void moveWidget(Widget widget, String destRegionName, int order) {
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        final String srcRegionName = widgetEntity.getRegion();
        List<Widget> srcWidgets = new ArrayList<Widget>(
                getWidgets(srcRegionName));
        srcWidgets.remove(widgetEntity.getOrder());
        reorderWidgets(srcWidgets);

        // Set the region to null otherwise the widget may be listed twice
        widgetEntity.setRegion(null);

        List<Widget> destWidgets = new ArrayList<Widget>(
                getWidgets(destRegionName));
        widgetEntity.setRegion(destRegionName);
        destWidgets.add(order, widgetEntity);
        reorderWidgets(destWidgets);
    }

    synchronized public void removeWidget(Widget widget) {
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        List<Widget> widgets = new ArrayList<Widget>(
                getWidgets(widgetEntity.getRegion()));
        widgets.remove(widgetEntity.getOrder());
        reorderWidgets(widgets);
        begin();
        em.remove(widget);
        commit();
    }

    public void reorderWidget(Widget widget, int order) {
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        List<Widget> widgets = new ArrayList<Widget>(
                getWidgets(widgetEntity.getRegion()));
        widgets.remove(widgetEntity.getOrder());
        widgets.add(order, widgetEntity);
        reorderWidgets(widgets);
    }

    public Map<String, String> getWidgetPreferences(Widget widget) {
        return ((WidgetEntity) widget).getPreferences();
    }

    public void setWidgetPreferences(Widget widget,
            Map<String, String> preferences) {
        begin();
        ((WidgetEntity) widget).setPreferences(preferences);
        commit();
    }

    public void setWidgetState(Widget widget, WidgetState state) {
        begin();
        ((WidgetEntity) widget).setState(state);
        commit();
    }

    synchronized private void reorderWidgets(List<Widget> widgets) {
        int i = 0;
        begin();
        for (Widget w : widgets) {
            em.merge(w);
            ((WidgetEntity) w).setOrder(i);
            i++;
        }
        commit();
    }

    synchronized public WidgetData getWidgetData(Widget widget, String dataName) {
        List<?> results = em.createNamedQuery("Data.findByWidgetAndName").setParameter(
                "widgetUid", widget.getUid()).setParameter("dataName", dataName).getResultList();
        DataEntity dataEntity = null;
        if (results.size() > 0) {
            dataEntity = (DataEntity) results.get(0);
            return dataEntity.getData();
        }
        return null;
    }

    synchronized public void setWidgetData(Widget widget, String dataName,
            WidgetData data) {
        List<?> results = em.createNamedQuery("Data.findByWidgetAndName").setParameter(
                "widgetUid", widget.getUid()).setParameter("dataName", dataName).getResultList();
        DataEntity dataEntity = null;
        if (results.size() > 0) {
            dataEntity = (DataEntity) results.get(0);
        } else {
            dataEntity = new DataEntity(widget.getUid(), dataName);
        }
        dataEntity.setData(data);
        begin();
        em.persist(dataEntity);
        commit();
    }

    synchronized public void deleteWidgetData(Widget widget) {
        begin();
        for (Object dataEntity : em.createNamedQuery("Data.findByWidget").setParameter(
                "widgetUid", widget.getUid()).getResultList()) {
            em.remove(dataEntity);
        }
        commit();
    }

    /*
     * Security
     */
    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return false;
        }
        return ((NuxeoPrincipal) currentNuxeoPrincipal).isAdministrator();
    }

    protected void begin() {
        if (et != null) {
            log.warn("transaction begin called twice");
        }
        et = em.getTransaction();
        if (!et.isActive()) {
            et.begin();
        }
    }

    protected void commit() {
        try {
            if (et != null && et.isActive()) {
                et.commit();
            }
        } finally {
            et = null;
        }
    }

}
