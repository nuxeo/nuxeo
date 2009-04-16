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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.ecm.platform.ec.placeful.service.ContainerManagedHibernateConfiguration;
import org.nuxeo.ecm.platform.ec.placeful.service.PersistenceProvider;
import org.nuxeo.ecm.platform.ec.placeful.service.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.platform.ec.placeful.service.PersistenceProvider.RunVoid;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class PlacefulServiceImpl extends DefaultComponent implements
        PlacefulService {

    private Map<String, String> registry;

    public static final PersistenceProvider persistenceProvider = new PersistenceProvider(
            new ContainerManagedHibernateConfiguration(
                    "jdbc/placeful_service_ds"));

    @Override
    public void activate(ComponentContext context) {
        registry = new HashMap<String, String>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            AnnotationDescriptor descriptor = (AnnotationDescriptor) contrib;
            for (String className : descriptor.getClassNames()) {
                String unqualifiedName = className.substring(className
                        .lastIndexOf('.') + 1);
                registry.put(unqualifiedName, className);
                persistenceProvider.addAnnotedClass(className);
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            AnnotationDescriptor descriptor = (AnnotationDescriptor) contrib;
            for (String className : descriptor.getClassNames()) {
                String unqualifiedName = className.substring(className
                        .lastIndexOf('.') + 1);
                registry.remove(unqualifiedName);
                persistenceProvider.removeAnnotedClass(className);
            }
        }
    }

    public Map<String, String> getAnnotationRegistry() {
        return registry;
    }

    public Annotation getAnnotation(EntityManager em, String id, String name) {
        String className = registry.get(name);
        String shortClassName = className
                .substring(className.lastIndexOf('.') + 1);
        Query query = em
                .createQuery("FROM " + shortClassName + " WHERE id=:id");
        query.setParameter("id", id);
        return (Annotation) query.getSingleResult();
    }

    public List<Annotation> getAllAnnotations(EntityManager em, String name) {
        String className = registry.get(name);
        String shortClassName = className
                .substring(className.lastIndexOf('.') + 1);
        Query query = em.createQuery("FROM " + shortClassName);
        return query.getResultList();
    }

    public List<Annotation> getAllAnnotations(final String name) {
        return persistenceProvider.run(false,
                new RunCallback<List<Annotation>>() {
                    public List<Annotation> runWith(EntityManager em) {
                        return getAllAnnotations(em, name);
                    }
                });
    }

    public Annotation getAnnotation(final String uuid, final String name) {
        return persistenceProvider.run(false, new RunCallback<Annotation>() {
            public Annotation runWith(EntityManager em) {
                return getAnnotation(em, uuid, name);
            }

        });
    }

    @SuppressWarnings({"unchecked"})
    public List<Annotation> getAnnotationListByParamMap(EntityManager em,
            Map<String, Object> paramMap, String name) {
        String className = registry.get(name);
        String shortClassName = className
                .substring(className.lastIndexOf('.') + 1);
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

    public List<Annotation> getAnnotationListByParamMap(
            final Map<String, Object> paramMap, final String name) {
        return persistenceProvider.run(false,
                new RunCallback<List<Annotation>>() {
                    public List<Annotation> runWith(EntityManager em) {
                        return getAnnotationListByParamMap(em, paramMap, name);
                    }

                });
    }

    public void removeAnnotationListByParamMap(EntityManager em,
            Map<String, Object> paramMap, String name) {

        List<Annotation> annotationsToRemove = getAnnotationListByParamMap(em,
                paramMap, name);
        if (annotationsToRemove != null && !annotationsToRemove.isEmpty()) {
            for (Annotation anno : annotationsToRemove) {
                if (anno != null) {
                    em.remove(anno);
                }
            }
        }
    }

    public void removeAnnotationListByParamMap(
            final Map<String, Object> paramMap, final String name) {
        persistenceProvider.run(true, new RunVoid() {
            public void runWith(EntityManager em) {
                removeAnnotationListByParamMap(em, paramMap, name);
            }

        });
    }

    public void setAnnotation(EntityManager em, Annotation annotation) {
        em.persist(annotation);
    }

    public void setAnnotation(final Annotation annotation) {
        persistenceProvider.run(true, new RunVoid() {
            public void runWith(EntityManager em) {
                setAnnotation(em, annotation);
            }

        });
    }

    public void removeAnnotation(EntityManager em, Annotation annotation) {
        em.remove(annotation);
    }

    public void removeAnnotation(final Annotation annotation) {
        persistenceProvider.run(true, new RunVoid() {
            public void runWith(EntityManager em) {
                removeAnnotation(em, annotation);
            }
        });
    }

    // protected void doRegisterHibernateOptions(HibernateOptionsDescriptor
    // desc) {
    // if (log.isDebugEnabled())
    // log.debug("Registered hibernate datasource : "
    // + desc.getDatasource());
    // hibernateConfiguration.setDescriptor(desc);
    // }

}
