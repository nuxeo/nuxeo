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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.core.cache;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CacheFeature.class })
public class CacheComplianceFixture {

    @Inject
    @Named("default")
    Cache defaultCache;

    @Test
    public void keyNotExist() throws IOException {
        Assert.assertNull(defaultCache.get("key-not-exist"));
        Assert.assertFalse(defaultCache.hasEntry("key-not-exist"));
    }

    @Test
    public void putUpdateGet() throws IOException {
        defaultCache.put("key", "val2");
        Assert.assertEquals("val2", defaultCache.get("key"));
    }

    @Test
    public void putNullKey() throws IOException {
        try {
            defaultCache.put(null, "val-null");
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    public void putNullValue() throws Exception {
        try {
            defaultCache.put("key-null", null);
            fail("Should raise exception !");
        } catch (Exception e) {
        }
    }

    @Test
    @Ignore
    public void ttlExpire() throws InterruptedException, IOException {
        defaultCache.put("key", "value");
        // Default config test set the TTL to 1mn, so wait 1mn and 1s
        Thread.sleep(61000);
        String expiredVal = (String) defaultCache.get("key");
        Assert.assertNull(expiredVal);
    }

    @Test
    public void invalidateKey() throws IOException {
        defaultCache.put("key","value");
        Assert.assertNotNull(defaultCache.get("key"));
        defaultCache.invalidate("key");
        Assert.assertNull(defaultCache.get("key"));
    }

    @Test
    public void invalidateAll() throws IOException {
        defaultCache.put("key","value");
        Assert.assertNotNull(defaultCache.get("key"));
        defaultCache.put("key2", "val2");
        Assert.assertNotNull(defaultCache.get("key2"));
        defaultCache.invalidateAll();
        Assert.assertNull(defaultCache.get("key"));
        Assert.assertNull(defaultCache.get("key2"));
    }

    @Test
    public void invalidateEmptyAll() throws IOException {
        defaultCache.invalidateAll();
    }
}
