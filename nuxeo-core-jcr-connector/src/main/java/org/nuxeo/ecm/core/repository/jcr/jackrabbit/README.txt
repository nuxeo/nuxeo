This custom search indexer is changing the way proxies are indexed:
instead of indexing the proxy itself it will index the target document version.

To install the custom SearchIndex class you should modify the
default/workspaces/default/workspace.xml
and replace the SearchIndex implementation with this one:

        <SearchIndex class="org.nuxeo.ecm.core.repository.jcr.jackrabbit.SearchIndex">
            <param name="path" value="${wsp.home}/index"/>
        </SearchIndex>
