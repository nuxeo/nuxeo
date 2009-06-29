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

import org.nuxeo.ecm.platform.ec.placeful.Annotation;
import org.nuxeo.ecm.platform.ec.placeful.PlacefulServiceImpl;
import org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces.PlacefulServiceLocal;
import org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces.PlacefulServiceRemote;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
@Stateless
@Local(PlacefulServiceLocal.class)
@Remote(PlacefulServiceRemote.class)
public class PlacefulServiceBean implements PlacefulServiceLocal, PlacefulServiceRemote {

    @PersistenceContext(unitName = "nxplaceful")
    protected EntityManager em;

    protected PlacefulServiceImpl service;

    @PostConstruct
    public void initialize() {
        service = (PlacefulServiceImpl) Framework.getRuntime().getComponent(PlacefulService.ID);
    }

    public Annotation getAnnotation(String id, String name) {
        return service.getAnnotation(em, id, name);
    }

    public List<Annotation> getAnnotationListByParamMap(
            Map<String, Object> paramMap, String name) {
        return service.getAnnotationListByParamMap(em, paramMap, name);
    }

    public void removeAnnotationListByParamMap(Map<String, Object> paramMap,
            String name) {
        service.removeAnnotationListByParamMap(em, paramMap, name);
    }

    public void setAnnotation(Annotation annotation) {
        service.setAnnotation(em, annotation);
    }

    public void removeAnnotation(Annotation annotation) {
        service.removeAnnotation(em,annotation);
    }

    public Map<String, String> getAnnotationRegistry() {
        return service.getAnnotationRegistry();
    }

}
