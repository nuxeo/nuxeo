package org.nuxeo.ecm.automation.features.upload;

import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.api.Framework;

@Operation(id = GetImportOptions.ID, category = Constants.CAT_SERVICES, label = "get import options", description = "Gives the list of possible import options given the contexte.")
public class GetImportOptions {

    public static final String ID = "ImportOptions.GET";

    @Context
    protected OperationContext ctx;

    @Param(name = "category", required = true)
    protected String category;

    @Param(name = "lang", required = false)
    protected String lang;

    @OperationMethod
    public Blob run() throws Exception {

        ImportOptionManager iom = Framework.getLocalService(ImportOptionManager.class);
        List<ImportOption> options = iom.getImportOptions(category);

        ResourceBundle rb = ResourceBundle.getBundle("messages");

        JSONArray rows = new JSONArray();
        for (ImportOption option : options) {
            JSONObject obj = new JSONObject();
            obj.element("name", option.getName());
            String label = null;
            try {
                label = rb.getString(option.getLabel());
            } catch (MissingResourceException e) {
                label=option.getLabel();
            }
            obj.element("label", label);
            String description = null;
            try {
                description = rb.getString(option.getDescription());
            } catch (MissingResourceException e) {
                description=option.getDescription();
            }
            obj.element("description", description);
            obj.element("operationId", option.getOperationId());
            if (option.getFormUrl()!=null) {
                obj.element("formUrl", option.getFormUrl());
            }
            rows.add(obj);
        }
        return new StringBlob(rows.toString(), "application/json");
    }

}
