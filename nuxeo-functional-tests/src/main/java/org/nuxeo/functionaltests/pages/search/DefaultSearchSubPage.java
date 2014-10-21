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
 */
package org.nuxeo.functionaltests.pages.search;

import java.util.Map;

import org.nuxeo.functionaltests.pages.search.aggregates.CheckBoxAggregateElements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.6
 */
public class DefaultSearchSubPage extends AbstractSearchSubPage {

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_coverage_agg")
    protected WebElement coverageAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_created_agg")
    protected WebElement createdAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_modified_agg")
    protected WebElement modifiedAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_common_size_agg")
    protected WebElement sizeAggregate;

    @FindBy(id = "nxl_gridSearchLayout:nxw_searchLayout_form:nxl_default_search_layout:nxw_dc_subjects_agg")
    protected WebElement subjectsAggregate;

    public DefaultSearchSubPage(WebDriver driver) {
        super(driver);
    }

    public Map<String, Integer> getAvailableCoverageAggregate() {
        return new CheckBoxAggregateElements(coverageAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableCreatedAggregate() {
        return new CheckBoxAggregateElements(createdAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableModifiedAggregate() {
        return new CheckBoxAggregateElements(modifiedAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableSizeAggregate() {
        return new CheckBoxAggregateElements(sizeAggregate).getAggregates();
    }

    public Map<String, Integer> getAvailableSubjectsAggregate() {
        return new CheckBoxAggregateElements(subjectsAggregate).getAggregates();
    }

    public WebElement getCoverageAggregate() {
        return coverageAggregate;
    }

    public WebElement getCreatedAggregate() {
        return createdAggregate;
    }

    public WebElement getModifiedAggregate() {
        return modifiedAggregate;
    }

    public WebElement getSizeAggregate() {
        return sizeAggregate;
    }

    public WebElement getSubjectsAggregate() {
        return subjectsAggregate;
    }

    public SearchPage selectCoverageAggregate(String label) {
        new CheckBoxAggregateElements(driver, coverageAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectCreatedAggregate(String label) {
        new CheckBoxAggregateElements(driver, createdAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectModifiedAggregate(String label) {
        new CheckBoxAggregateElements(driver, modifiedAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectSizeAggregate(String label) {
        new CheckBoxAggregateElements(driver, sizeAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }

    public SearchPage selectSubjectsAggregate(String label) {
        new CheckBoxAggregateElements(driver, subjectsAggregate).selectOrUnselect(label);
        return asPage(SearchPage.class);
    }
}
