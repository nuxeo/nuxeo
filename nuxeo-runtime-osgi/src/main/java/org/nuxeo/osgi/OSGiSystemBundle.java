package org.nuxeo.osgi;

import java.util.Properties;

import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;

public class OSGiSystemBundle extends OSGiBundleHost implements Framework {

    protected final Properties properties;

    public OSGiSystemBundle(OSGiSystemBundleFile file, Properties props)
            throws BundleException {
        super(file);
        properties = props;
    }

    @Override
    public void init() throws BundleException {
        osgi = new OSGiSystemContext(this, properties);
        osgi.registry.register(this);
    }


    @Override
    public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
        return new FrameworkEvent(RESOLVED, this, null);
    }


}
