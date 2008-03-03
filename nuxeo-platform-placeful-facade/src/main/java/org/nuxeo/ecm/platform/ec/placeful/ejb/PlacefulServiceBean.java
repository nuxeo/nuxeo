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
 * $Id: PlacefulServiceBean.java 28924 2008-01-10 14:04:05Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful.ejb;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.nuxeo.ecm.platform.ec.placeful.Annotation;
import org.nuxeo.ecm.platform.ec.placeful.PlacefulServiceImpl;
import org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces.PlacefulServiceLocal;
import org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces.PlacefulServiceRemote;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
@Stateless
@Local(PlacefulServiceLocal.class)
@Remote(PlacefulServiceRemote.class)
public class PlacefulServiceBean implements PlacefulServiceLocal, PlacefulServiceRemote {

    @PersistenceContext(unitName = "nxplacefulservice")
    protected EntityManager em;

    protected PlacefulService service;

    @PostConstruct
    public void initialize() {
        service = (PlacefulService) Framework.getRuntime().getComponent(PlacefulServiceImpl.ID);
    }

    public Annotation getAnnotation(String id, String name) throws ClassNotFoundException {
        String className = service.getAnnotationRegistry().get(name);
        //Class klass = Thread.currentThread().getContextClassLoader().loadClass(className);
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);
        Query query = em.createQuery("FROM " + shortClassName + " WHERE id=:id");
        query.setParameter("id", id);
        return (Annotation) query.getSingleResult();
    }

    public List<Annotation> getAnnotationListByParamMap(
            Map<String, Object> paramMap, String name) throws ClassNotFoundException {
        String className = service.getAnnotationRegistry().get(name);
        //Class klass = Thread.currentThread().getContextClassLoader().loadClass(className);
        String shortClassName = className.substring(className.lastIndexOf('.') + 1);
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

    public void removeAnnotationListByParamMap(Map<String, Object> paramMap,
            String name) throws ClassNotFoundException {

        List<Annotation> annotationsToRemove = getAnnotationListByParamMap(
                paramMap, name);
        if (annotationsToRemove != null && !annotationsToRemove.isEmpty()) {
            for (Annotation anno : annotationsToRemove) {
                if (anno != null) {
                    em.remove(anno);
                }
            }
        }
    }

    public void setAnnotation(Annotation annotation) {
        em.persist(annotation);
    }

    public void removeAnnotation(Annotation annotation) {
        em.remove(annotation);
    }

}
