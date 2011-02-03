package org.nuxeo.ecm.platform.ui.web;

import org.nuxeo.ecm.platform.ui.web.util.beans.PropertiesEditorsInstaller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class UIWebActivator implements BundleActivator {

    PropertiesEditorsInstaller editorsInstaller = new PropertiesEditorsInstaller();

    @Override
    public void start(BundleContext context) throws Exception {
        editorsInstaller.installEditors();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        editorsInstaller.uninstallEditors();
    }

}
