/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @since 7.1
 */
public class BodyFactoryScanner {

    protected static final Log log = LogFactory.getLog(BodyFactoryScanner.class);

    public Set<BodyFactory> getFactories(BundleContext ctx) {
        Set<BodyFactory> result = new TreeSet<BodyFactory>(new BodyFactoryComparator());
        result.addAll(getFactory(ctx.getBundle()));
        return result;
    }

    protected List<BodyFactory> getFactory(Bundle bundle) {
        List<BodyFactory> result = new ArrayList<BodyFactory>();
        Enumeration entries = bundle.findEntries("/", "*.class", true);
        String rootPath = bundle.getEntry("/").getPath();
        log.warn("rootPAth : " + rootPath);
        while (entries.hasMoreElements()) {
            String entryPath = ((URL) entries.nextElement()).getPath();
            log.warn(entryPath);
            String entryName = entryPath.substring(rootPath.length() + 1, entryPath.length() - ".class".length()).replace(
                    '/', '.');
            log.warn(entryName);
            try {
                Class<?> entryClass = bundle.loadClass(entryName);
                if (BodyFactory.class.isAssignableFrom(entryClass) && !entryClass.isAnonymousClass() && !entryClass.isInterface()) {
                    log.warn("Discovered BodyFactory : " + entryName);
                    result.add((BodyFactory) entryClass.newInstance());
                }
            } catch (ClassNotFoundException e) {
                log.warn(e.getMessage());
            } catch (InstantiationException e) {
                log.warn(e.getMessage());
            } catch (IllegalAccessException e) {
                log.warn(e.getMessage());
            }
        }
        return result;
    }

    public BodyFactory scan(BundleContext context) {
        final Set<MessageBodyReader<?>> messageBodyReaders = new LinkedHashSet<MessageBodyReader<?>>();
        final Set<MessageBodyWriter<?>> messageBodyWriters = new LinkedHashSet<MessageBodyWriter<?>>();
        for (BodyFactory bf : getFactories(context)) {
            messageBodyReaders.addAll(bf.getMessageBodyReaders());
            messageBodyWriters.addAll(bf.getMessageBodyWriters());
        }
        return new BodyFactory() {

            @Override
            public Set<MessageBodyReader<?>> getMessageBodyReaders() {
                return messageBodyReaders;
            }

            @Override
            public Set<MessageBodyWriter<?>> getMessageBodyWriters() {
                return messageBodyWriters;
            }

            @Override
            public int getPriority() {
                return 0;
            }

        };
    }

}
