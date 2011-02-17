package com.nuxeo.ecm.usersettings.types;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.usersettings.UserSettingsType;

public class UserSettingsWrapper {

    private DocumentModel document;

    public void setUser(String user) throws ClientException {
        document.setProperty(UserSettingsType.SCHEMA,
                UserSettingsType.PROP_USER, user);
    }

    public String getUser() throws ClientException {
        return (String) document.getProperty(UserSettingsType.SCHEMA,
                UserSettingsType.PROP_USER);
    }

    public UserSettingsWrapper(DocumentModel document) throws ClientException {
        if (document == null)
            throw new ClientException(
                    "Forbidden to instanciate a wrapper with null document");
        this.document = document;
    }

}
