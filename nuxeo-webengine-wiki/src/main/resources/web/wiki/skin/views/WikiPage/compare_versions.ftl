<#import "common/util.ftl" as base/>

<#assign r1 = Context.request.getParameter('r1') />
<#assign r2 = Context.request.getParameter('r2') />
<#assign l1 = Session.getDocument(docRef(r1)).versionLabel />
<#assign l2 = Session.getDocument(docRef(r2)).versionLabel />
<#assign c1 = Session.getDocument(docRef(r1)).wikiPage.content />
<#assign c2 = Session.getDocument(docRef(r2)).wikiPage.content />

<@extends src="base.ftl">
<@block name="content">

<script language="JavaScript" src="${skinPath}/script/diff_match_patch.js"></script>
<script language="JavaScript" src="${skinPath}/script/diff.js"></script>

<script language="JavaScript">
$(document).ready(function(){    
  doCompare();
});
</script>

<h1>Compare rev${l1} and rev${l2} of ${Document.title}</h1>




<div id="revisions">
        <div class="hidden"  id="rev1" width="80" rows="15">${c1?html}</div>
        <div class="hidden" id="rev2" width="80" rows="15">${c2?html}</div>
        <div id="diffoutput" class="diff"></div>
        <div id="difftime" class="small"></div>
</div>
</@block>
</@extends>
