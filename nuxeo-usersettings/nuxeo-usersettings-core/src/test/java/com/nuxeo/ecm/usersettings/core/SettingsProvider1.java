package com.nuxeo.ecm.usersettings.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.usersettings.UserSettingsProvider;

public class SettingsProvider1 implements UserSettingsProvider {

    public static final String TEST_PROPERTY_VALUE = "Test Property 1";

    private static final String TEST_TYPE = "TestSettings1";

    private static final long serialVersionUID = -2643607647191744171L;

    public DocumentModelList getUserSettings(DocumentModel userWorkspace,
            CoreSession coreSession, String userName) throws ClientException {

        DocumentModelList list = coreSession.getChildren(
                userWorkspace.getRef(), TEST_TYPE);

        if (list.size() == 0) {

            DocumentModel dm = coreSession.createDocumentModel(
                    userWorkspace.getPathAsString(), "testsettings1", TEST_TYPE);

            dm.setProperty("testsettings1", "testprop1", TEST_PROPERTY_VALUE);

            DocumentModel doc = coreSession.createDocument(dm);

            coreSession.save();

            list.add(doc);

        }

        return list;

    }

    public Boolean isTypeSupported(String type) {

        return TEST_TYPE.equals(type);

    }

    public Boolean isTypeSupported(DocumentType type) {

        return isTypeSupported(type.getName());

    }

}
