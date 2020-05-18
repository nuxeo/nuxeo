<#macro fulltextFilter name action>
  <#if false>
  <!--
  params:
   - name: Displayed name in the placeholder.
   - action: name of the resource that can make a fulltext search; for instance `listExtensionPoints`
             from `org.nuxeo.apidoc.browse.ApiBrowser#filterExtensionPoints`
  -->
  </#if>
  <form id="fulltext" method="POST" action="${Root.path}/${distId}/${action}">
    <input name="fulltext" id="fulltext-box" type="search" class="searchFilter"
    placeholder="Find in ${name}"<#if searchFilter??> value="${searchFilter}"</#if> autofocus />
    <input id="filter-submit-button" type="submit" value="Search"/>
  </form>
</#macro>

<#macro tableFilterArea name>
  <#if false>
  <!--
  params:
   - name: Displayed name in the placeholder
  -->
  </#if>
  <input name="fulltext" id="filter-box" type="search" placeholder="Filter ${name}" class="searchFilter" autofocus />
</#macro>


<#macro googleSearchFrame criterion>
<div class="googleSearchFrame">
  <script>
    (function() {
      var render = function() {
        var params = {
          div: 'gsr',
          tag: 'searchresults-only',
          gname: 'gsr'
        };

        google.search.cse.element.render(params);
        google.search.cse.element.getElement(params.gname).execute('${criterion?js_string}');
      };

      var gseCallback = function() {
        if (document.readyState === 'complete') {
          render();
        } else {
          google.setOnLoadCallback(render, true);
        }
      };

      window.__gcse = {
        parsetags: 'explicit',
        callback: gseCallback
      };

      var cx = '012209002953539942917:sa7nmpwkglq';
      var gcse = document.createElement('script');
      gcse.type = 'text/javascript';
      gcse.async = true;
      gcse.src = 'https://cse.google.com/cse.js?cx=' + cx;
      var s = document.getElementsByTagName('script')[0];
      s.parentNode.insertBefore(gcse, s);
    })();
  </script>
  <div id="gsr"></div>
</div>
</#macro>

<#macro tableSortFilterScript name sortList>
<script type="text/javascript">
  $(document).ready(function() {
    $("${name}")
    .tablesorter({sortList: [${sortList}], widgets: ['zebra'], cancelSelection: false})
    <#if !searchFilter??>
    .tablesorterFilter({
      filterContainer: "#filter-box",
      filterClearContainer: "#filter-clear-button",
      filterWaitTime: 600
    })
    </#if>
  });

  $('#filter-box').click(function(e) {
    e.stopPropagation();
  });

  $('#fulltext').children().click(function(e) {
    e.stopPropagation();
  });
</script>
</#macro>

<#macro toc>
  <div id="tocDiv" class="subnav">
    <ul id="tocList">
    </ul>
  </div>
</#macro>

<#macro tocTrigger>
  <script>
    $(document).ready(function() {
      $.fn.toc("#tocList");
    });
  </script>
</#macro>
