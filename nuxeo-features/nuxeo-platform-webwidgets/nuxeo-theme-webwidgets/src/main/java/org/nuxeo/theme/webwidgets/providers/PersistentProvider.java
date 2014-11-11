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
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.webwidgets.Provider;
import org.nuxeo.theme.webwidgets.ProviderException;
import org.nuxeo.theme.webwidgets.Widget;
import org.nuxeo.theme.webwidgets.WidgetData;
import org.nuxeo.theme.webwidgets.WidgetState;

public class PersistentProvider implements Provider {

    private static final Log log = LogFactory.getLog(PersistentProvider.class);

    protected PersistenceProvider persistenceProvider;

    public void activate() {
        PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
        persistenceProvider = persistenceProviderFactory.newProvider("nxwebwidgets");
        persistenceProvider.openPersistenceUnit();
    }

    public void deactivate() {
        if (persistenceProvider != null) {
            persistenceProvider.closePersistenceUnit();
            persistenceProvider = null;
        }
    }

    @Override
    public void destroy() throws ProviderException {
        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    em.createQuery("DELETE FROM DataEntity data").executeUpdate();
                    for (Object w : em.createQuery("FROM WidgetEntity widget").getResultList()) {
                        em.remove(w);
                    }
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }
    }

    public PersistenceProvider getPersistenceProvider() {
        if (persistenceProvider == null) {
            activate();
        }
        return persistenceProvider;
    }

    public Principal getCurrentPrincipal() {
        WebContext ctx = WebEngine.getActiveContext();
        if (ctx != null) {
            return ctx.getPrincipal();
        }
        return null;
    }

    public void addWidget(final Widget widget, final String regionName,
            final int order) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (regionName == null) {
            throw new ProviderException("Region name is undefined");
        }

        List<Widget> widgets = getWidgets(regionName);
        widgets.add(order, widget);
        reorderWidgets(widgets);

        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    ((WidgetEntity) widget).setRegion(regionName);
                    em.merge(widget);
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }
    }

    public synchronized Widget createWidget(final String widgetTypeName)
            throws ProviderException {
        if (widgetTypeName == null) {
            throw new ProviderException("Widget type name is undefined");
        }

        try {
            return getPersistenceProvider().run(true,
                    new RunCallback<Widget>() {
                        public Widget runWith(EntityManager em) {
                            final WidgetEntity widget = new WidgetEntity(
                                    widgetTypeName);
                            em.persist(widget);
                            return widget;
                        }
                    });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }
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

    public synchronized Widget getWidgetByUid(final String uid)
            throws ProviderException {
        Widget widget;
        try {
            widget = getPersistenceProvider().run(false,
                    new RunCallback<Widget>() {
                        public Widget runWith(EntityManager em) {
                            Widget widget = em.find(WidgetEntity.class,
                                    Integer.valueOf(uid));
                            return widget;
                        }
                    });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

        if (widget == null) {
            throw new ProviderException("Widget not found: " + uid);
        }
        return widget;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<Widget> getWidgets(final String regionName)
            throws ProviderException {
        if (regionName == null) {
            throw new ProviderException("Region name is undefined.");
        }

        try {
            return getPersistenceProvider().run(true,
                    new RunCallback<List<Widget>>() {
                        public List<Widget> runWith(EntityManager em) {
                            Query query = em.createNamedQuery("Widget.findAll");
                            query.setParameter("region", regionName);
                            return query.getResultList();
                        }
                    });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

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
        List<Widget> srcWidgets = getWidgets(srcRegionName);
        srcWidgets.remove(widget);
        reorderWidgets(srcWidgets);

        // Set the region to null otherwise the widget may be listed twice
        widgetEntity.setRegion(null);

        List<Widget> destWidgets = getWidgets(destRegionName);
        widgetEntity.setRegion(destRegionName);
        destWidgets.add(order, widgetEntity);
        reorderWidgets(destWidgets);
    }

    public synchronized void removeWidget(final Widget widget)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        final int id = widgetEntity.getId();
        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    WidgetEntity w = em.getReference(WidgetEntity.class, id);
                    em.remove(w);
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }
        List<Widget> widgets = getWidgets(widgetEntity.getRegion());
        reorderWidgets(widgets);
    }

    public void reorderWidget(Widget widget, int order)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        WidgetEntity widgetEntity = (WidgetEntity) widget;
        List<Widget> widgets = getWidgets(widgetEntity.getRegion());
        widgets.remove(widget);
        widgets.add(order, widget);
        reorderWidgets(widgets);
    }

    public Map<String, String> getWidgetPreferences(Widget widget)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        return ((WidgetEntity) widget).getPreferences();
    }

    public void setWidgetPreferences(final Widget widget,
            final Map<String, String> preferences) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (preferences == null) {
            throw new ProviderException("Widget preferences are undefined");
        }

        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    ((WidgetEntity) widget).setPreferences(preferences);
                    em.merge(widget);
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

    }

    public void setWidgetState(final Widget widget, final WidgetState state)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (state == null) {
            throw new ProviderException("Widget state is undefined");
        }

        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    ((WidgetEntity) widget).setState(state);
                    em.merge(widget);
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

    }

    public synchronized void reorderWidgets(final List<Widget> widgets)
            throws ProviderException {
        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    int i = 0;
                    for (Widget w : widgets) {
                        WidgetEntity widget = ((WidgetEntity) w);
                        int order = widget.getOrder();
                        if (order != i) {
                            ((WidgetEntity) w).setOrder(i);
                            em.merge(w);
                        }
                        i = i + 1;
                    }
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }
    }

    public synchronized WidgetData getWidgetData(final Widget widget,
            final String dataName) throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (dataName == null || "".equals(dataName)) {
            throw new ProviderException("Data name is undefined");
        }

        try {
            return getPersistenceProvider().run(true,
                    new RunCallback<WidgetData>() {
                        public WidgetData runWith(EntityManager em) {
                            Query query = em.createNamedQuery("Data.findByWidgetAndName");
                            query.setParameter("widgetUid", widget.getUid());
                            query.setParameter("dataName", dataName);
                            List<?> results = query.getResultList();
                            if (results.size() > 0) {
                                DataEntity dataEntity = (DataEntity) results.get(0);
                                return dataEntity.getData();
                            }
                            return null;
                        }

                    });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

    }

    public synchronized void setWidgetData(final Widget widget,
            final String dataName, final WidgetData data)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }
        if (dataName == null || "".equals(dataName)) {
            throw new ProviderException("Data name is undefined");
        }

        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    Query query = em.createNamedQuery("Data.findByWidgetAndName");
                    query.setParameter("widgetUid", widget.getUid());
                    query.setParameter("dataName", dataName);
                    List<?> results = query.getResultList();
                    final DataEntity dataEntity = results.size() > 0 ? (DataEntity) results.get(0)
                            : new DataEntity(widget.getUid(), dataName);
                    dataEntity.setData(data);
                    em.merge(dataEntity);
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

    }

    public synchronized void deleteWidgetData(final Widget widget)
            throws ProviderException {
        if (widget == null) {
            throw new ProviderException("Widget is undefined");
        }

        try {
            getPersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    Query query = em.createNamedQuery("Data.findByWidget");
                    query.setParameter("widgetUid", widget.getUid());
                    for (Object dataEntity : query.getResultList()) {
                        em.remove(dataEntity);
                    }
                }
            });
        } catch (ClientException e) {
            throw new ProviderException(e);
        }

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

}
