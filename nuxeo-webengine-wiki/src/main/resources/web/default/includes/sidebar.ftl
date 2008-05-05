<div class="sideblock">
    <h3>Search</h3>
    <form action="${root.docURL}@@search" method="get" accept-charset="utf-8">
        <input class="complete" type="text" name="q" id="q" autosave="com.mysite" results="5">
        <input type="hidden" name="p" value="${root.path}">
    </form>
</div>


<div class="sideblock">
    <h3>Last Items</h3>
    <ul>
        <#list root.children?reverse as entry>
            <li><a href="${entry.name}">${entry.title}</a></li>
            <#if entry_index = 3><#break></#if>
        </#list>
    </ul>
</div>

<!-- See other related document should contributed by the wiki-->
<div class="sideblock">
  <@block name="seeother"/>
</div>

<!-- let other templates add more things to the sidebar -->
<@block name="sidebar"/>

<div class="sideblock">
<h3>Actions</h3>
<ul>
<#list this.actions as action>
<li> ${message("action."+action.id)}
</#list>
</ul>


<li><a href="${this.docURL}@@edit">Edit</a></li>
<li><a href="${this.docURL}@@view">View</a></li>
<li><a href="${this.docURL}@@show_versions">History</a></li>

</div>