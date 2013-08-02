package org.nuxeo.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

public class OSGiSystemActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        OSGiSystemContext osgi = (OSGiSystemContext) context;
        osgi.setThreadContextClassLoader();
        osgi.registerService(PackageAdmin.class, new OSGiPackageAdmin(osgi),
                null);
        osgi.registry.resolve();
    }


    @Override
    public void stop(BundleContext context) throws Exception {
        OSGiSystemContext osgi = (OSGiSystemContext) context;
        osgi.resetThreadContextClassLoader();
    }

}
