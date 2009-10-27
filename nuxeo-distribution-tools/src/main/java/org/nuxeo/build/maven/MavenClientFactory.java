/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.maven;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MavenClientFactory {

    private static boolean isEmbedded;
    private static MavenClient instance;
    private static ThreadLocal<MavenClient> threadInstance = new ThreadLocal<MavenClient>(); 
    
    public static void setInstance(MavenClient client) {
        if (client instanceof EmbeddedMavenClient) {
            isEmbedded = true;
            instance = client;
        } else {
            isEmbedded = false;
            threadInstance.set(client);
        }
    }
    
    public static boolean isEmbedded() {
        return isEmbedded;
    }
    
    public static MavenClient getInstance() {
        if (isEmbedded) {
            return instance;
        } else {
            return threadInstance.get();
        }
    }
    
    public static EmbeddedMavenClient getEmbeddedMaven() {
        if (isEmbedded) {
            return (EmbeddedMavenClient)instance;
        }
        throw new IllegalStateException("Not an embedded maven client. You can run this task only in embedded mode");
    }
    
}
