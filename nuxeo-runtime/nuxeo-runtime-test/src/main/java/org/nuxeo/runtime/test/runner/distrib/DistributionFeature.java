/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner.distrib;

import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DistributionFeature extends SimpleFeature {

//    protected NuxeoDistribution distrib;
//    protected NuxeoApp app;
//
//    @Override
//    public void initialize(FeaturesRunner runner) throws Exception {
//        distrib = FeaturesRunner.getScanner().getFirstAnnotation(runner.getTargetTestClass(), NuxeoDistribution.class);
//        if (distrib == null) {
//            throw new IllegalArgumentException("No distribution specified. Use @NuxeoDistribution on your class to specify the distribution to be used");
//        }
//
//    }
//
//    @Override
//    public void start(FeaturesRunner runner) throws Exception {
//        File home = makeHome(distrib.home());
//        app = new NuxeoApp(home);
//        app.setVerbose(true);
//        app.setOffline(distrib.offline());
//        app.setUpdatePolicy(distrib.updatePolicy());
//        if (distrib.config().length() > 0) {
//            app.build(makeUrl(distrib.config()), distrib.useCache());
//        } else {
//            app.build(distrib.profile(), distrib.useCache());
//        }
//        NuxeoApp.setHttpServerAddress(distrib.host(), distrib.port());
//        app.start();
//    }
//
//    protected File makeHome(String path) {
//        if (path.startsWith("~")) {
//            path = System.getProperty("user.home") + path.substring(1);
//        }
//        path = path.replace("{profile}", distrib.profile());
//        path = path.replace("{tmp}", System.getProperty("java.io.tmpdir"));
//        return new File(path);
//    }
//
//    @Override
//    public void stop(FeaturesRunner runner) throws Exception {
//        app.shutdown();
//    }
//
//
//    protected static URL makeUrl(String spec) {
//        try {
//        if (spec.indexOf(':') > -1) {
//            if (spec.startsWith("java:")) {
//                spec = spec.substring(5);
//                ClassLoader cl = getContextClassLoader();
//                URL url = cl.getResource(spec);
//                if (url == null) {
//                    fail("Canot found java resource: "+spec);
//                }
//                return url;
//            } else {
//                return new URL(spec);
//            }
//        } else {
//            return new File(spec).toURI().toURL();
//        }
//        } catch (Exception e) {
//            fail("Invalid config file soecification. Not a valid URL or file: "+spec);
//            return null;
//        }
//    }
//
//    protected static void fail(String msg) {
//        System.err.println(msg);
//        System.exit(2);
//    }
//
//    protected static ClassLoader getContextClassLoader() {
//        ClassLoader cl = Thread.currentThread().getContextClassLoader();
//        return cl == null ? DistributionFeature.class.getClassLoader() : cl;
//    }

}
