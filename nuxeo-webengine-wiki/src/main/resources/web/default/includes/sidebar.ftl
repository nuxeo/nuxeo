<div class="sideblock general">
  <p class="createButton">
   <a href="/nuxeo/site/${Root.name}@@create_entry"><span>Create NOW!</span></a>
  </p>
</div>

<!-- let other templates add more things to the sidebar -->

  <@block name="sidebar"/>

  <!-- See other related document should contributed by the wiki-->
  <!--  JQuery-needed : appears only if wiki page contains seeother -->
  <div class="sideblock contextual">
    <h3>Related documents</h3>
    <div class="sideblock-content">
      <@block name="seeother"/>
    </div>  
  </div>
  <div class="sideblock-content-bottom"></div>

<div class="sideblock general">
    <h3>Last Items</h3>
    <ul>
        <#list Root.children?reverse as entry>
            <li><a href="/nuxeo/site/${Root.name}/${entry.name}">${entry.title}</a></li>
            <#if entry_index = 3><#break></#if>
        </#list>
    </ul>
</div>



