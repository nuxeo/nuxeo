<!-- tinyMCE -->
<script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/langs/en.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/themes/simple/editor_template.js"></script>
<!-- end tinyMCE -->

<form name="blogPostEdit" method="POST" action="${This.path}/modifyWebPage" accept-charset="utf-8">
  <table class="modifyWebPage">
    <tbody>
      <tr>
        <td>${Context.getMessage("label.blogpost.title")}</td>
      </tr>
      <tr>
        <td><input type="text" size="60" name="title" value="${Document.title}"/></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.blogpost.description")}</td>
      </tr>
      <tr>
        <td><textarea name="description" cols="80">${Document.dublincore.description}</textarea></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.blogpost.content")}</td>
      </tr>
      <tr>
        <td>
          <textarea name="richtextEditorEdit" style="width:300px; height:400px" cols="80" rows="20"
          	id="richtextEditorEdit">${Document.webpage.content}</textarea>
        </td>
      </tr>
      <tr>
        <td colspan="2"><input type="hidden" name="isRichtext" id="wikitext" value="true"/>
      </tr>
      <tr>
        <td colspan="2">
          <input type="submit" class="button" value="${Context.getMessage("action_save")}" /> &nbsp;
          <input type="button" class="button" value="${Context.getMessage("action_cancel")}" onclick="document.blogPostEdit.action='${This.path}/@perspective/view'; document.blogPostEdit.submit();" />
        </td>
      </tr>
    </tbody>
  </table>
</form>

<script type="text/javascript">
$('#richtextEditorEdit').ready(function() {
  document.tmceEdit = new tinymce.Editor('richtextEditorEdit',{
  	mode : "textareas",
  	theme : "advanced",
  	editor_selector : "mceAdvanced"
  });

  document.tmceEdit.render();
});
</script>