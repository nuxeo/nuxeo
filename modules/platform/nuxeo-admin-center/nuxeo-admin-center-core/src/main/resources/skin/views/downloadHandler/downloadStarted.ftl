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
 var idParam = url.indexOf("?");
 var returnUrl = url.substring(0,idx) + "/packages/${source?xml}?filterOnPlatform=${filterOnPlatform?xml}&amp;type=${type?xml}&amp;onlyRemote=${onlyRemote?xml}" + url.substring(idParam,url.length);
 document.location.href=returnUrl;
}

function install() {
 stopRefresh=true;
 var url = document.location.href;
 var idx = url.indexOf("/download/");
 url = url.substring(0,idx) + "/install/start/${pkg.id}/?source=${source?xml}&depCheck=${depCheck?xml}"
 document.location.href=url;
}

</script>

</@block>

<@block name="body">

  <div class="downloadDiv">

    <#if (over || pkg.completed)>
      <div class="successfulDownloadBox">
        <p>${Context.getMessage('label.downloadStarted.title.start')} ${pkg.id} ${Context.getMessage('label.downloadStarted.title.end')}</p>
        <#if !install>
	        <script>
	         window.setTimeout(back,1000);
	        </script>
	    </#if>
        <#if install>
	        <script>
	         window.setTimeout(install,500);
	        </script>
	    </#if>
      </div>
    </#if>
    <#if (!over && !pkg.completed)>
      <div class="inProgressDownloadBox">
        <p>${Context.getMessage('label.downloadStarted.progress.mess1')} ${pkg.id} ${Context.getMessage('label.downloadStarted.progress.mess2')} : ${pkg.getDownloadProgress()} % </p>
        <div class="downloadProgressContainer alignCenter">
          <div class="downloadProgressBar alignCenter" style="width:${pkg.getDownloadProgress()}px;">&nbsp;</div>
        </div>
        <div class="downloadSize">
        ${Context.getMessage('label.downloadStarted.size.label')} ${pkg.getSourceSize()}
        </div>
        <script>
         window.setTimeout(refresh,1500);
        </script>
        <p><a href="javascript:back()">${Context.getMessage('label.downloadStarted.progress.background')}</a></p>
      </div>
    </#if>

  </div>

</@block>
</@extends>
