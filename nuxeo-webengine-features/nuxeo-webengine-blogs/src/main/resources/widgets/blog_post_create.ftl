<!-- tinyMCE -->
<script type="text/javascript" src="${skinPath}/script/jquery/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce_init.js"></script>
<!-- end tinyMCE -->

<script type="text/javascript">
	tinymce.baseURL = "${skinPath}/script/tiny_mce";
    function isTitleSet() {
        var title = document.getElementById('titleInput');
        if (title) {
            if (title.value == "") {
                alert("Please insert a valid title.");
                return false;
            }
        }
        return true;
    }
</script>

<form name="blogPostCreate" method="POST" onsubmit="return isTitleSet();"
  action="${This.path}/createWebPage" accept-charset="utf-8">
<input type="hidden" name="pageName" value="${Context.request.getAttribute('pageName')}" />
<table class="createWebPage">
  <tbody>
  <tr>
    <td>${Context.getMessage("label.blogpost.title")}</td>
  </tr>
  <tr>
    <td><input type="text" size="60" id="titleInput" name="title" value="${Context.request.getAttribute('pageName')}" /></td>
   </tr>
   <tr>
      <td>${Context.getMessage("label.blogpost.description")}</td>
    </tr>
    <tr>
      <td><textarea name="description" cols="80"></textarea></td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.blogpost.content")}</td>
    </tr>
    <tr>
      <td>
        <textarea name="richtextEditor" class="mceEditor" style="width:300px; height:400px" cols="80" rows="20" id="richtextEditor"></textarea>
      </td>
    </tr>
    <tr>
      <td colspan="2"><input type="hidden" name="isRichtext" id="wikitext" value="true"/>
    </tr>
    <tr>
      <td colspan="2">
        <input type="submit" class="button" value="${Context.getMessage("action_save")}" />&nbsp;
        <input type="button" class="button" value="${Context.getMessage("action_cancel")}" onclick="document.blogPostCreate.action='${This.path}/@perspective/view'; document.blogPostCreate.submit();" />
      </td>
    </tr>
  </tbody>
</table>
</form>