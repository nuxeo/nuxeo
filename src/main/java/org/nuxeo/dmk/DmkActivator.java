package org.nuxeo.dmk;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sun.jdmk.comm.AuthInfo;
import com.sun.jdmk.comm.HtmlAdaptorServer;

public class DmkActivator implements BundleActivator {

    protected ObjectName adaptorName;

    protected HtmlAdaptorServer adaptor;

    protected ObjectName httpConnectorName;

    protected JMXConnectorServer httpConnector;

    protected ObjectName httpsConnectorName;

    protected JMXConnectorServer httpsConnector;

    @Override
    public void start(BundleContext arg0) throws Exception {
        final Log log = LogFactory.getLog(DmkActivator.class);
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // adaptor
        adaptor = new HtmlAdaptorServer();
        adaptor.addUserAuthenticationInfo(new AuthInfo("Administrator", "pfouh"));
        adaptor.setPort(8081);
        adaptor.start();
        adaptor.
        adaptorName = new ObjectName(
                "org.nuxeo:name=jmx-adaptor");
        mbs.registerMBean(adaptor, adaptorName);

        log.info("JMX http adaptor available at port 8081");
        // http connector
//        JMXServiceURL httpURL = new JMXServiceURL("jdmk-http", null, 6868);
//        httpConnector = JMXConnectorServerFactory.newJMXConnectorServer(
//                httpURL, null, mbs);
//        httpConnectorName = new ObjectName(
//                "org.nuxeo:type=jmx-connector,protocol=jdmk-http");
//        mbs.registerMBean(httpConnector, httpConnectorName);
//        log.info("jmx-http connector available at " + httpConnector.getAddress());

        // https connector
//        JMXServiceURL httpsURL = new JMXServiceURL("jdmk-https", null, 6869);
//        httpsConnector = JMXConnectorServerFactory.newJMXConnectorServer(
//                httpsURL, null, mbs);
//        httpsConnectorName = new ObjectName(
//                "org.nuxeo:type=jmx-connector,protocol=jdmk-https");
//        log.info("jmx-https connector available at " + httpsConnector.getAddress());

//
//        mbs.registerMBean(httpsConnector, httpsConnectorName);

    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            if (adaptor.isActive()) {
                adaptor.stop();
            }
            mbs.unregisterMBean(
                    adaptorName);
        } finally {
            adaptor = null;
            adaptorName = null;
        }

//        try {
//            if (httpConnector.isActive()) {
//                httpConnector.stop();
//            }
//            mbs.unregisterMBean(
//                    httpConnectorName);
//        } finally {
//            httpConnector = null;
//            httpConnectorName = null;
//        }

//        try {
//            if (httpsConnector.isActive()) {
//                httpsConnector.stop();
//            }
//            mbs.unregisterMBean(
//                    httpsConnectorName);
//        } finally {
//            httpsConnector = null;
//            httpsConnectorName = null;
//        }

        adaptorName = null;
        adaptor = null;

    }
    
    

}
