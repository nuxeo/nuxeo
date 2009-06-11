/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.tag.persistence;

import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_AUTHOR;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_CREATION_DATE;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_DOCUMENT_ID;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_ID;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_IS_PRIVATE;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_COLUMN_TAG_ID;
import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.TAGGING_TABLE_NAME;

import java.sql.Types;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.nuxeo.ecm.platform.tag.entity.DublincoreEntity;
import org.nuxeo.ecm.platform.tag.entity.HierarchyEntity;
import org.nuxeo.ecm.platform.tag.entity.TagEntity;
import org.nuxeo.ecm.platform.tag.entity.TaggingEntity;
import org.nuxeo.ecm.platform.tag.sql.Column;
import org.nuxeo.ecm.platform.tag.sql.Table;
import org.nuxeo.runtime.api.Framework;

/**
 * The persistence provider used for getting an <b>EntityManager</b> needed to
 * handle queries on the table that is used for tagging support.
 *
 * @author cpriceputu
 */
public class TagPersistenceProvider {

    private EntityManagerFactory emf;

    private Properties properties;

    private static TagPersistenceProvider _instance;

    private TagPersistenceProvider() {
    }

    public static final TagPersistenceProvider getInstance() {
        if (_instance == null) {
            _instance = new TagPersistenceProvider();
        }
        return _instance;
    }

    private Properties getProperties() {
        if (null == properties) {
            properties = new Properties();
            properties.put("hibernate.show_sql",
                    Framework.getProperty("hibernate.show_sql"));
            properties.put("hibernate.connection.driver_class",
                    Framework.getProperty("hibernate.connection.driver_class"));
            properties.put("hibernate.connection.username",
                    Framework.getProperty("hibernate.connection.username"));
            properties.put("hibernate.connection.password",
                    Framework.getProperty("hibernate.connection.password"));
            properties.put("hibernate.connection.url",
                    Framework.getProperty("hibernate.connection.url"));
            properties.put("hibernate.dialect",
                    Framework.getProperty("hibernate.dialect"));
        }
        return properties;
    }

    /**
     * Method used to get the <b>EntityManagerFactor</b> that is used to obtain
     * an application-managed entity manager.
     */
    protected void openPersistenceUnit() throws MappingException,
            HibernateException {
        try {
            Ejb3Configuration cfg = new Ejb3Configuration();
            cfg.configure("tagservice-hibernate.cfg.xml");

            cfg.addProperties(properties);
            cfg.addAnnotatedClass(TaggingEntity.class);
            cfg.addAnnotatedClass(DublincoreEntity.class);
            cfg.addAnnotatedClass(TagEntity.class);
            cfg.addAnnotatedClass(HierarchyEntity.class);
            emf = cfg.buildEntityManagerFactory();
        } catch (Exception e) {
            throw new HibernateException(e);
        }
    }

    /**
     * Method used to close the entity manager factory.It is indicated to do
     * this.
     */
    public void closePersistenceUnit() {
        if (emf == null) {
            return;
        }
        if (emf.isOpen()) {
            emf.close();
        }
        emf = null;
        _instance = null;
    }

    /**
     * Returns an <b>EntityManager</b> that manage itself the transaction
     * processes.
     *
     * @return
     */
    public EntityManager getEntityManager(Properties properties) {
        if (emf == null || !emf.isOpen()) {
            if (null == properties) {
                properties = getProperties();
            }

            this.properties = properties;
            openPersistenceUnit();
        }
        return emf.createEntityManager();
    }

    /**
     * Commit all the changes that are kept by an <b>EntityManager</b> created
     * with an active transaction.
     *
     * @param em
     */
    public void doCommit(EntityManager em) {
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            return;
        }
        em.flush();
        et.commit();
    }

    /**
     * Rolls back all the changes that are kept by an <b>EntityManager</b>
     * created with an active transaction.
     *
     * @param em
     */
    public void doRollback(EntityManager em) {
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            return;
        }
        em.flush();
        et.rollback();
    }

    public void createTableTagging(EntityManager em) {

        try {
            Dialect dialect = (Dialect) Class.forName(
                    getProperties().get("hibernate.dialect").toString()).newInstance();

            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
                em.createNativeQuery(getCreateSql(dialect)).executeUpdate();
                doCommit(em);
            } else {
                em.createNativeQuery(getCreateSql(dialect)).executeUpdate();
            }
        } catch (Exception e) {
            // table exists already
            doRollback(em);
        }

    }

    private String getCreateSql(Dialect dialect) {
        Table table = new Table(TAGGING_TABLE_NAME);
        Column column = new Column(TAGGING_TABLE_COLUMN_ID, Types.VARCHAR);
        column.setPrimary(true);
        column.setNullable(false);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_TAG_ID, Types.CLOB);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_AUTHOR, Types.VARCHAR);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_DOCUMENT_ID, Types.CLOB);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_CREATION_DATE, Types.DATE);
        table.addColumn(column);
        column = new Column(TAGGING_TABLE_COLUMN_IS_PRIVATE, Types.BOOLEAN);
        table.addColumn(column);
        return table.getCreateSql(dialect);
    }
}
