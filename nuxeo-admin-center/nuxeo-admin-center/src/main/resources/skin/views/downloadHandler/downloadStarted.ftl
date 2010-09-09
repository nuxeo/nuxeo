<@extends src="base.ftl">

<@block name="header_scripts">

<script>

var stopRefresh=false;

function refresh() {
 if (stopRefresh) {
  return;
 }
 var url = document.location.href.replace("/start/", "/progressPage/");
 document.location.href=url;
}

function back() {
 stopRefresh=true;
 var url = document.location.href;
 var idx = url.indexOf("/download/");
 url = url.substring(0,idx) + "/packages/${source}"
 document.location.href=url;
}

</script>

</@block>

<@block name="body">

  <div class="downloadDiv">

    <#if (over || pkg.completed)>
      <div class="successfulDownloadBox">
        <p>Download for package ${pkg.id} terminated </p>
        <script>
         window.setTimeout(back,1000);
        </script>
      </div>
    </#if>
    <#if (!over && !pkg.completed)>
      <div class="inProgressDownloadBox">
        <p>Download for package ${pkg.id} is in progress : ${pkg.getDownloadProgress()} % </p>
        <div class="downloadProgressContainer alignCenter">
          <div class="downloadProgressBar alignCenter" style="width:${pkg.getDownloadProgress()}px;">&nbsp;</div>
        </div>
        <div class="downloadSize">
        Total package size : ${pkg.getSourceSize()} bytes
        </div>
        <script>
         window.setTimeout(refresh,1500);
        </script>
        <p><a href="javascript:back()"> Let download continue in background </a></p>
      </div>
    </#if>

  </div>

</@block>
</@extends>