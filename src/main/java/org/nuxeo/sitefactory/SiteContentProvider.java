package org.nuxeo.sitefactory;

import java.util.List;

import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.sitefactory.api.ContentEntry;
import org.nuxeo.sitefactory.api.NavigationAxis;
import org.nuxeo.sitefactory.api.NavigationEntry;

public interface SiteContentProvider {

    // Navigation providers
    
    List<NavigationAxis> getNavigationAxes();
    
    List<NavigationEntry> getNavigationEntryForAxis(String axisName);
    
    List<NavigationEntry> getNavigationSubEntries(String navEntryId);
    
    // Content providers
    
    PageProvider<ContentEntry> getContentProvider();
    
    PageProvider<ContentEntry> getContentProviderForAxis(String axisName);
    
    PageProvider<ContentEntry> getContentProviderForNavigation(String navEntryId);
    
    
    List<Tag> getAllTags();
    
    PageProvider<ContentEntry> getTaggedContentProvider(List<Tag> tags);    
    
}
