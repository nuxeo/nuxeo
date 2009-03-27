<#macro search>

 <div class="searchService" >
   <form id="search" action="${This.path}/search" method="POST" accept-charset="utf-8">      
     <input class="directoryFilter" type="text" name="searchParam"  size="15"/>
     <input class="button" type="submit" name="search_page" value="Search" id="search_page"/>
   </form>
 </div>

</#macro> 