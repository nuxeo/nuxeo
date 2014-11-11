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
 * $Id: PlacefulServiceTest.java 19071 2007-05-21 16:20:16Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.ec.placeful.PlacefulServiceImpl;
import org.nuxeo.ecm.platform.ec.placeful.SubscriptionConfig;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;

/**
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
public class PlacefulServiceTest extends TestCase {
    private static final String ID = "0000-1111-2222-3333";
    private static final String EVENT = "publish";
    private static final String ID_1 = "4444-5555-6666-7777";
    private static final String EVENT_1 = "reject";
    private static final String ANNOTATION_NAME = "SubscriptionConfig";

    private EntityManagerFactory emf;
    private EntityManager em;
    private PlacefulService service;

    @Override
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("nxplacefulservice");
        em = emf.createEntityManager();
        createTestData();

        service = new PlacefulServiceImpl() {
            @Override
            public Map<String, String> getAnnotationRegistry() {
                Map<String, String> registry = new HashMap<String, String>();
                registry.put(ANNOTATION_NAME,
                        "org.nuxeo.ecm.platform.ec.placeful.SubscriptionConfig");
                return registry;
            }
        };
    }

    @Override
    public void tearDown() {
        if (em != null) {
            removeTestData();
            em.close();
        }
        if (emf != null) {
            emf.close();
        }
    }

    private void createTestData() {
        SubscriptionConfig annotation = new SubscriptionConfig();
        annotation.setId(ID);
        annotation.setEvent(EVENT);
        em.getTransaction().begin();
        em.persist(annotation);
        em.getTransaction().commit();
    }

    private void removeTestData() {
        em.getTransaction().begin();
        SubscriptionConfig annotation = em.find(SubscriptionConfig.class, ID);
        if (annotation != null) {
            em.remove(annotation);
        }
        em.getTransaction().commit();
    }

    public void testGetAnnotation() {
        PlacefulServiceBean serviceBean = new PlacefulServiceBean();

        serviceBean.em = em;
        serviceBean.service = (PlacefulServiceImpl) service;
        SubscriptionConfig annotation = (SubscriptionConfig) serviceBean
                .getAnnotation(ID, ANNOTATION_NAME);
        assertNotNull(annotation);
        assertEquals(ID, annotation.getId());
        assertEquals(EVENT, annotation.getEvent());
    }

    public void testSetAnnotation() {
        PlacefulServiceBean serviceBean = new PlacefulServiceBean();
        serviceBean.em = em;
        serviceBean.service = (PlacefulServiceImpl) service;
        SubscriptionConfig annotation = new SubscriptionConfig();
        annotation.setId(ID_1);
        annotation.setEvent(EVENT_1);

        em.getTransaction().begin();
        serviceBean.setAnnotation(annotation);
        annotation = (SubscriptionConfig) serviceBean.getAnnotation(ID_1,
                ANNOTATION_NAME);
        em.getTransaction().rollback();

        assertNotNull(annotation);
        assertEquals(ID_1, annotation.getId());
        assertEquals(EVENT_1, annotation.getEvent());
    }

}
