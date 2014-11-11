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
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.HibernateConfigurator;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.TransactionTypeHelper;
import org.nuxeo.theme.webwidgets.Provider;
import org.nuxeo.theme.webwidgets.ProviderException;
import org.nuxeo.theme.webwidgets.Widget;
import org.nuxeo.theme.webwidgets.WidgetData;
import org.nuxeo.theme.webwidgets.WidgetState;

public class PersistentProvider implements Provider {

    private static final class UnsupportedEntityManager implements EntityManager {
        public void clear() {
            throw new UnsupportedOperationException();
        }

        public void close() {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object arg0) {
            throw new UnsupportedOperationException();
        }

        public Query createNamedQuery(String arg0) {
            throw new UnsupportedOperationException();
        }

        public Query createNativeQuery(String arg0) {
            throw new UnsupportedOperationException();
        }

        public Query createNativeQuery(String arg0, Class arg1) {
            throw new UnsupportedOperationException();
        }

        public Query createNativeQuery(String arg0, String arg1) {
            throw new UnsupportedOperationException();
        }

        public Query createQuery(String arg0) {
            throw new UnsupportedOperationException();
        }

        public <T> T find(Class<T> arg0, Object arg1) {
            throw new UnsupportedOperationException();
        }

        public void flush() {
            throw new UnsupportedOperationException();
        }

        public Object getDelegate() {
            throw new UnsupportedOperationException();
        }

        public FlushModeType getFlushMode() {
            throw new UnsupportedOperationException();
        }

        public <T> T getReference(Class<T> arg0, Object arg1) {
            throw new UnsupportedOperationException();
        }

        public EntityTransaction getTransaction() {
            throw new UnsupportedOperationException();
        }

        public boolean isOpen() {
            throw new UnsupportedOperationException();
        }

        public void joinTransaction() {
            throw new UnsupportedOperationException();
        }

        public void lock(Object arg0, LockModeType arg1) {
            throw new UnsupportedOperationException();
        }

        public <T> T merge(T arg0) {
            throw new UnsupportedOperationException();
        }

        public void persist(Object arg0) {
            throw new UnsupportedOperationException();
        }

        public void refresh(Object arg0) {
            throw new UnsupportedOperationException();
        }

        public void remove(Object arg0) {
            throw new UnsupportedOperationException();
        }

        public void setFlushMode(FlushModeType arg0) {
            throw new UnsupportedOperationException();
        }
    }

    private static final Log log = LogFactory.getLog(PersistentProvider.class);

    protected EntityManager em = new UnsupportedEntityManager();

    protected EntityTransaction et;

    public void activate() {
        HibernateConfigurator configurator;
        try {
            configurator = Framework.getService(HibernateConfigurator.class);
        } catch (Exception e) {
            log.error("No hibernate configurator available, aborting", e);
            return;
        }
        HibernateConfiguration config = configurator.getHibernateConfiguration("nxwebwidgets");
        em = config.getFactory(TransactionTypeHelper.RESOURCE_LOCAL).createEntityManager();
    }

    public Principal getCurrentPrincipal() {
        WebContext ctx = WebEngine.getActiveContext();
        if (ctx != null) {
            return ctx.getPrincipal();
        }
        return null;
    }

    public void addWidget(Widget widget, String regionName, int order)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (regionName == null) {
            throw new ProviderException("Region name is undefined");
        }
        List<Widget> widgets = new ArrayList<Widget>(getWidgets(regionName));
        ((WidgetEntity) widget).setRegion(regionName);
        widgets.add(order, widget);
        reorderWidgets(widgets);
    }

    public synchronized Widget createWidget(String widgetTypeName)
            throws ProviderException {
        if (widgetTypeName == null) {
            throw new ProviderException("Widget type name is undefined");
        }
        final WidgetEntity widget = new WidgetEntity(widgetTypeName);
        begin();
        em.persist(widget);
        commit();
        return widget;
    }

    public String getRegionOfWidget(Widget widget) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        return ((WidgetEntity) widget).getRegion();
    }

    public WidgetState getWidgetState(Widget widget) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        return ((WidgetEntity) widget).getState();
    }

    public synchronized Widget getWidgetByUid(String uid)
            throws ProviderException {
        Widget widget = em.find(WidgetEntity.class, Integer.valueOf(uid));
        if (widget == null) {
            throw new ProviderException("Widget not found: " + uid);
        }
        return widget;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<Widget> getWidgets(String regionName)
            throws ProviderException {
        if (regionName == null) {
            throw new ProviderException("Region name is undefined.");
        }
        List<Widget> widgets = new ArrayList<Widget>();
        widgets.addAll(em.createNamedQuery("Widget.findAll").setParameter(
                "region", regionName).getResultList());
        return widgets;
    }

    public void moveWidget(Widget widget, String destRegionName, int order)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (destRegionName == null) {
            throw new ProviderException("Destination region name is undefined.");
        }
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

    public synchronized void removeWidget(Widget widget)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        List<Widget> widgets = new ArrayList<Widget>(
                getWidgets(widgetEntity.getRegion()));
        widgets.remove(widgetEntity.getOrder());
        reorderWidgets(widgets);
        begin();
        em.remove(widget);
        commit();
    }

    public void removeWidgets() throws ProviderException {
        begin();
        Query query = em.createNamedQuery("Widget.removeAll");
        query.executeUpdate();
        commit();
    }

    public void reorderWidget(Widget widget, int order)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        List<Widget> widgets = new ArrayList<Widget>(
                getWidgets(widgetEntity.getRegion()));
        widgets.remove(widgetEntity.getOrder());
        widgets.add(order, widgetEntity);
        reorderWidgets(widgets);
    }

    public Map<String, String> getWidgetPreferences(Widget widget)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        return ((WidgetEntity) widget).getPreferences();
    }

    public void setWidgetPreferences(Widget widget,
            Map<String, String> preferences) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (preferences == null) {
            throw new ProviderException("Widget preferences are undefined");
        }
        begin();
        ((WidgetEntity) widget).setPreferences(preferences);
        commit();
    }

    public void setWidgetState(Widget widget, WidgetState state)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (state == null) {
            throw new ProviderException("Widget state is undefined");
        }
        begin();
        ((WidgetEntity) widget).setState(state);
        em.persist(widget);
        commit();
    }

    protected synchronized void reorderWidgets(List<Widget> widgets) {
        int i = 0;
        begin();
        for (Widget w : widgets) {
            em.merge(w);
            ((WidgetEntity) w).setOrder(i);
            i++;
        }
        commit();
    }

    public synchronized WidgetData getWidgetData(Widget widget, String dataName)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (dataName == null || "".equals(dataName)) {
            throw new ProviderException("Data name is undefined");
        }
        List<?> results = em.createNamedQuery("Data.findByWidgetAndName").setParameter(
                "widgetUid", widget.getUid()).setParameter("dataName", dataName).getResultList();
        if (results.size() > 0) {
            DataEntity dataEntity = (DataEntity) results.get(0);
            return dataEntity.getData();
        }
        return null;
    }

    public synchronized void setWidgetData(Widget widget, String dataName,
            WidgetData data) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (dataName == null || "".equals(dataName)) {
            throw new ProviderException("Data name is undefined");
        }
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

    public synchronized void deleteWidgetData(Widget widget)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
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
        Principal currentNuxeoPrincipal = getCurrentPrincipal();
        if (currentNuxeoPrincipal == null) {
            log.warn("Could not get the current user from the context.");
            return false;
        }
        return ((NuxeoPrincipal) currentNuxeoPrincipal).isMemberOf(SecurityConstants.ADMINISTRATORS);
    }

    /*
     * Transactions
     */
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
