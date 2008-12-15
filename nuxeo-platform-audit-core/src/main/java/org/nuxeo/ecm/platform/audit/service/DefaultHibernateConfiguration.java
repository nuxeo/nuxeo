package org.nuxeo.ecm.platform.audit.service;

import java.util.Properties;


public class DefaultHibernateConfiguration implements HibernateConfiguration {

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.connection.autocommit", true);
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.hbm2ddl.auto", "true");
        properties.put("hibernate.connection.datasource", "java:/nxaudit-logs");
        return properties;
    }

}
