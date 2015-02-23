/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.wro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.nuxeo.ecm.web.resources.wro.factory.NuxeoWroCacheKeyFactory;

import ro.isdc.wro.cache.CacheKey;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.manager.factory.BaseWroManagerFactory;
import ro.isdc.wro.manager.factory.WroManagerFactory;
import ro.isdc.wro.model.group.GroupExtractor;
import ro.isdc.wro.model.group.processor.InjectorBuilder;
import ro.isdc.wro.model.resource.ResourceType;

/**
 * @since 7.3
 */
public class TestNuxeoCacheKeyFactory {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private GroupExtractor mockGroupExtractor;

    private NuxeoWroCacheKeyFactory victim;

    @BeforeClass
    public static void onBeforeClass() {
        assertEquals(0, Context.countActive());
    }

    @AfterClass
    public static void onAfterClass() {
        assertEquals(0, Context.countActive());
    }

    @Before
    public void setUp() {
        initMocks(this);
        Context.set(Context.standaloneContext());
        victim = new NuxeoWroCacheKeyFactory();
        final WroManagerFactory managerFactory = new BaseWroManagerFactory().setGroupExtractor(mockGroupExtractor);
        InjectorBuilder.create(managerFactory).build().inject(victim);
    }

    @After
    public void tearDown() {
        Context.unset();
    }

    @Test
    public void shouldHaveMinimizeEnabledByDefault() {
        assertEquals(true, Context.get().getConfig().isMinimizeEnabled());
    }

    @Test(expected = NullPointerException.class)
    public void cannotBuildCacheKeyFromNullRequest() {
        victim.create(null);
    }

    @Test
    public void shouldCreateNullCacheKeyWhenRequestDoesNotContainEnoughInfo() {
        assertNull(victim.create(mockRequest));
    }

    @Test
    public void shouldCreateNullCacheKeyWhenRequestDoesNotContainResourceType() {
        when(mockGroupExtractor.getGroupName(mockRequest)).thenReturn("g1");
        when(mockGroupExtractor.getResourceType(mockRequest)).thenReturn(null);
        assertNull(victim.create(mockRequest));
    }

    @Test
    public void shouldCreateNullCacheKeyWhenRequestDoesNotGroupName() {
        when(mockGroupExtractor.getGroupName(mockRequest)).thenReturn(null);
        when(mockGroupExtractor.getResourceType(mockRequest)).thenReturn(ResourceType.CSS);
        assertNull(victim.create(mockRequest));
    }

    @Test
    public void shouldCreateValidCacheKeyWhenRequestContainsAllRequiredInfo() {
        when(mockGroupExtractor.isMinimized(mockRequest)).thenReturn(true);
        when(mockGroupExtractor.getGroupName(mockRequest)).thenReturn("g1");
        when(mockGroupExtractor.getResourceType(mockRequest)).thenReturn(ResourceType.CSS);
        assertEquals(new CacheKey("g1", ResourceType.CSS, true), victim.create(mockRequest));
    }

    @Test
    public void shouldHaveMinimizationTurnedOffWhenMinimizeEnabledIsFalse() throws IOException {
        when(mockGroupExtractor.isMinimized(mockRequest)).thenReturn(true);
        when(mockGroupExtractor.getGroupName(mockRequest)).thenReturn("g1");
        when(mockGroupExtractor.getResourceType(mockRequest)).thenReturn(ResourceType.CSS);
        Context.get().getConfig().setMinimizeEnabled(false);
        assertEquals(new CacheKey("g1", ResourceType.CSS, false), victim.create(mockRequest));
    }

    @Test
    public void shouldChangeWithFlavor() throws IOException {
        Context.get().getConfig().setMinimizeEnabled(false);
        when(mockGroupExtractor.getGroupName(mockRequest)).thenReturn("g1");
        when(mockGroupExtractor.getResourceType(mockRequest)).thenReturn(ResourceType.CSS);

        when(mockRequest.getQueryString()).thenReturn("flavor=foo");
        CacheKey fooKey = new CacheKey("g1", ResourceType.CSS, false);
        fooKey.addAttribute("flavor", "foo");
        assertEquals(fooKey, victim.create(mockRequest));

        when(mockRequest.getQueryString()).thenReturn("flavor=bar");
        CacheKey barKey = new CacheKey("g1", ResourceType.CSS, false);
        barKey.addAttribute("flavor", "bar");
        assertEquals(barKey, victim.create(mockRequest));

        assertNotEquals(barKey, fooKey);
    }

    @Test
    public void shouldChangeWithParams() throws IOException {
        Context.get().getConfig().setMinimizeEnabled(false);
        when(mockGroupExtractor.getGroupName(mockRequest)).thenReturn("g1");
        when(mockGroupExtractor.getResourceType(mockRequest)).thenReturn(ResourceType.CSS);

        when(mockRequest.getQueryString()).thenReturn("flavor=foo");
        CacheKey fooKey = new CacheKey("g1", ResourceType.CSS, false);
        fooKey.addAttribute("flavor", "foo");
        assertEquals(fooKey, victim.create(mockRequest));

        when(mockRequest.getQueryString()).thenReturn("flavor=foo&bar=blah");
        CacheKey barKey = new CacheKey("g1", ResourceType.CSS, false);
        barKey.addAttribute("flavor", "foo");
        barKey.addAttribute("bar", "blah");
        assertEquals(barKey, victim.create(mockRequest));

        assertNotEquals(barKey, fooKey);
    }

}
