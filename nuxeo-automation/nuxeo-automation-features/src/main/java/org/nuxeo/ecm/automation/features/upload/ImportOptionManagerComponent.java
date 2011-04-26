package org.nuxeo.ecm.automation.features.upload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ImportOptionManagerComponent extends DefaultComponent implements ImportOptionManager {

    protected Map<String, List<ImportOption>> importOptions = new HashMap<String, List<ImportOption>>();

    protected final static String EP_IMPORTOPTION = "importOption";

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {
        if (EP_IMPORTOPTION.equals(extensionPoint)) {
            synchronized (importOptions) {
                ImportOption option = (ImportOption) contribution;

                if (!importOptions.containsKey(option.getCategory())) {
                    importOptions.put(option.getCategory(), new ArrayList<ImportOption>());
                }
                importOptions.get(option.getCategory()).add(option);
                Collections.sort(importOptions.get(option.getCategory()));
            }
        }
    }

    @Override
    public List<ImportOption> getImportOptions(String dropZone) {
        return importOptions.get(dropZone);
    }

}
