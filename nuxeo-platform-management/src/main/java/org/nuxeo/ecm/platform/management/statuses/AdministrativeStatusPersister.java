package org.nuxeo.ecm.platform.management.statuses;

public interface AdministrativeStatusPersister {
	
	String setValue(String serverInstanceName, String value);
	String getValue(String serverInstanceName);

}
