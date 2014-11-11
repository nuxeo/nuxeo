/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: PlacefulServiceImpl.java 19072 2007-05-21 16:23:42Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.HibernateConfigurator;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class PlacefulServiceImpl extends DefaultComponent implements PlacefulService {

    protected final Map<String, String> registry = new HashMap<String, String>();

    protected static final Log log = LogFactory.getLog(PlacefulServiceImpl.class);

    protected PersistenceProvider persistenceProvider;

    protected HibernateConfiguration hibernateConfiguration;

    public PersistenceProvider getOrCreatePersistenceProvider() {
        if (persistenceProvider != null) {
            return persistenceProvider;
        }
        PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
        return persistenceProvider = persistenceProviderFactory.newProvider("nxplaceful");
    }

    public HibernateConfiguration getOrCreateHibernateConfiguration() {
        if (hibernateConfiguration != null) {
            return hibernateConfiguration;
        }
        HibernateConfigurator hibernateConfigurator = Framework.getLocalService(HibernateConfigurator.class);
        return hibernateConfiguration = hibernateConfigurator.getHibernateConfiguration("nxplaceful");
    }

    protected void deactivatePersistenceProvider() {
        if (persistenceProvider == null) {
            return;
        }
        persistenceProvider.closePersistenceUnit();
        persistenceProvider = null;
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        deactivatePersistenceProvider();
        registry.clear();
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) throws Exception {
        if ("annotations".equals(extensionPoint)) {
            registerAnnotations((AnnotationDescriptor) contribution);
        }
    }

    protected void registerAnnotations(AnnotationDescriptor contribution) {
        HibernateConfiguration config = getOrCreateHibernateConfiguration();
        for (Class<? extends Annotation> annotationClass : contribution.annotationClasses) {
            String canonicalName = annotationClass.getCanonicalName();
            String unqualifiedName = canonicalName.substring(canonicalName.lastIndexOf('.') + 1);
            registry.put(unqualifiedName, annotationClass.getCanonicalName());
            config.addAnnotedClass(annotationClass);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) throws Exception {
        if (extensionPoint.equals("annotations")) {
            unregisterAnnotations((AnnotationDescriptor) contribution);
        }
    }

    protected void unregisterAnnotations(AnnotationDescriptor contribution) {
        HibernateConfiguration config = getOrCreateHibernateConfiguration();
        for (Class<? extends Annotation> annotationClass : contribution.annotationClasses) {
            String canonicalName = annotationClass.getCanonicalName();
            String unqualifiedName = canonicalName.substring(canonicalName.lastIndexOf('.') + 1);
            registry.remove(unqualifiedName);
            config.removeAnnotedClass(annotationClass);
        }
    }

    public Map<String, String> getAnnotationRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    public static String getShortName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public Annotation getAnnotation(EntityManager em, String id, String name) {
        String className = registry.get(name);
        String shortClassName = getShortName(className);
        Query query = em.createQuery("FROM " + shortClassName + " WHERE id=:id");
        query.setParameter("id", id);
        return (Annotation) query.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Annotation> getAllAnnotations(EntityManager em, String name) {
        String className = registry.get(name);
        String shortClassName = getShortName(className);
        Query query = em.createQuery("FROM " + shortClassName);
        return (List<Annotation>) query.getResultList();
    }

    public List<Annotation> getAllAnnotations(final String name) {
        try {
            return getOrCreatePersistenceProvider().run(false, new RunCallback<List<Annotation>>() {
                public List<Annotation> runWith(EntityManager em) {
                    return getAllAnnotations(em, name);
                }
            });
        } catch (ClientException e) {
           throw new ClientRuntimeException(e);
        }
    }

    public Annotation getAnnotation(final String uuid, final String name) {
        try {
            return getOrCreatePersistenceProvider().run(false, new RunCallback<Annotation>() {
                public Annotation runWith(EntityManager em) {
                    return getAnnotation(em, uuid, name);
                }

            });
        } catch (ClientException e) {
           throw new ClientRuntimeException(e);
        }
    }

    @SuppressWarnings( { "unchecked" })
    public List<Annotation> getAnnotationListByParamMap(EntityManager em, Map<String, Object> paramMap, String name) {
        String className = registry.get(name);
        if (className == null) {
            // add fail safe
            log.warn("No placeful configuration registred for " + name);
            return new ArrayList<Annotation>();
        }
        String shortClassName = getShortName(className);
        StringBuilder queryString = new StringBuilder("FROM " + shortClassName);
        if (paramMap != null && !paramMap.isEmpty()) {
            queryString.append(" WHERE ");
            int size = paramMap.size();
            int index = 1;
            for (String key : paramMap.keySet()) {
                queryString.append(key + "=:" + key);
                if (index != size) {
                    queryString.append(" and ");
                }
                index++;
            }
        }
        Query query = em.createQuery(queryString.toString());

        if (paramMap != null && !paramMap.isEmpty()) {
            for (String key : paramMap.keySet()) {
                query.setParameter(key, paramMap.get(key));
            }
        }

        return query.getResultList();
    }

    public List<Annotation> getAnnotationListByParamMap(final Map<String, Object> paramMap, final String name) {
        try {
            return getOrCreatePersistenceProvider().run(false, new RunCallback<List<Annotation>>() {
                public List<Annotation> runWith(EntityManager em) {
                    return getAnnotationListByParamMap(em, paramMap, name);
                }

            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
         }
    }

    public void removeAnnotationListByParamMap(EntityManager em, Map<String, Object> paramMap, String name) {

        List<Annotation> annotationsToRemove = getAnnotationListByParamMap(em, paramMap, name);
        if (annotationsToRemove != null && !annotationsToRemove.isEmpty()) {
            for (Annotation anno : annotationsToRemove) {
                if (anno != null) {
                    em.remove(anno);
                }
            }
        }
    }

    public void removeAnnotationListByParamMap(final Map<String, Object> paramMap, final String name) {
        try {
            getOrCreatePersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    removeAnnotationListByParamMap(em, paramMap, name);
                }

            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
         }
    }

    public void setAnnotation(EntityManager em, Annotation annotation) {
        em.persist(annotation);
    }

    public void setAnnotation(final Annotation annotation) {
        try {
            getOrCreatePersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    setAnnotation(em, annotation);
                }

            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
         }
    }

    public void removeAnnotation(EntityManager em, Annotation annotation) {
        em.remove(annotation);
    }

    public void removeAnnotation(final Annotation annotation) {
        try {
            getOrCreatePersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    removeAnnotation(em, annotation);
                }
            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
         }
    }
}
