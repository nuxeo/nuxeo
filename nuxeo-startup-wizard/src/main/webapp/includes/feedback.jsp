<%
if (ctx.hasErrors()) {%>
    <div class="errBlock">
    <%
    Map<String,String> errors = ctx.getErrorsMap();
    for (String field : errors.keySet()) {%>
  <div class="errItem" id="err_<%=field%>"><fmt:message key="<%=errors.get(field)%>" /></div>
  <% } %>
 </div>
<script>
  var fieldsInError = <%=ctx.getFieldsInErrorAsJson()%>;

  $(document).ready( function () {
    for ( var i = 0; i < fieldsInError.length; i++) {
        var selector = 'input[name*="' + fieldsInError[i] + '"]';
        $(selector).css('border-color','red');
        //$(selector).title('border-color','red')
    }
  });
</script>
<%}%>
