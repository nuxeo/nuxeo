/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 *     Miguel Nixo
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationServiceImpl;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.awt.geom.Point2D;
import java.io.IOException;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFWatermarkingTranslationTest {

    private static long PAGE_WIDTH = 400;
    private static long PAGE_HEIGHT = 800;
    private static long WATERMARK_WIDTH = 200;
    private static long WATERMARK_HEIGHT = 100;

    @Inject
    PDFTransformationServiceImpl pdfTransformationService;

    @Test
    public void testBottomLeftCorner() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(0,vector.getX(),1L);
        Assert.assertEquals(0,vector.getY(),1L);
    }

    @Test
    public void testBottomRightCorner() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(PAGE_WIDTH-WATERMARK_WIDTH,vector.getX(),1L);
        Assert.assertEquals(0,vector.getY(),1L);
    }

    @Test
    public void testTopRightCorner() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(PAGE_WIDTH-WATERMARK_WIDTH,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT-WATERMARK_HEIGHT,vector.getY(),1L);
    }

    @Test
    public void testTopRightCornerWithMargin() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setxPosition(50);
        properties.setyPosition(50);
        properties.setInvertX(true);
        properties.setInvertY(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(PAGE_WIDTH-WATERMARK_WIDTH-50,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT-WATERMARK_HEIGHT-50,vector.getY(),1L);
    }

    @Test
    public void testTopLeftCorner() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertY(true);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(0,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT-WATERMARK_HEIGHT,vector.getY(),1L);
    }

    @Test
    public void testCenter() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals((PAGE_WIDTH-WATERMARK_WIDTH)/2,vector.getX(),1L);
        Assert.assertEquals((PAGE_HEIGHT-WATERMARK_HEIGHT)/2,vector.getY(),1L);
    }

    @Test
    public void testBottomLeftCornerRotationDown() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(-90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(0,vector.getX(),1L);
        Assert.assertEquals(WATERMARK_WIDTH,vector.getY(),1L);
    }

    @Test
    public void testBottomLeftCornerRotationDownWithMargin() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(-90);
        properties.setxPosition(50);
        properties.setyPosition(50);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(50,vector.getX(),1L);
        Assert.assertEquals(WATERMARK_WIDTH+50,vector.getY(),1L);
    }

    @Test
    public void testBottomLeftCornerRotationUp() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(WATERMARK_HEIGHT,vector.getX(),1L);
        Assert.assertEquals(0,vector.getY(),1L);
    }

    @Test
    public void testTopRightCornerRotationDown() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        properties.setRotation(-90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(PAGE_WIDTH-WATERMARK_HEIGHT,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT,vector.getY(),1L);
    }

    @Test
    public void testTopRightCornerRotationUp() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(PAGE_WIDTH,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT-WATERMARK_WIDTH,vector.getY(),1L);
    }

    @Test
    public void testTopRightCornerRotationUpWithMargin() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setInvertX(true);
        properties.setInvertY(true);
        properties.setxPosition(50);
        properties.setyPosition(50);
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals(PAGE_WIDTH-50,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT-WATERMARK_WIDTH-50,vector.getY(),1L);
    }

    @Test
    public void testCenterRotationUp() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        properties.setRotation(90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals((PAGE_WIDTH+WATERMARK_HEIGHT)/2,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT/2-WATERMARK_WIDTH/2,vector.getY(),1L);
    }

    @Test
    public void testCenterRotationDown() throws IOException {
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        properties.setRotation(-90);
        Point2D vector = pdfTransformationService.computeTranslationVector(
                PAGE_WIDTH,WATERMARK_WIDTH,PAGE_HEIGHT,WATERMARK_HEIGHT,
                properties);
        Assert.assertEquals((PAGE_WIDTH-WATERMARK_HEIGHT)/2,vector.getX(),1L);
        Assert.assertEquals(PAGE_HEIGHT/2+WATERMARK_WIDTH/2,vector.getY(),1L);
    }

}
