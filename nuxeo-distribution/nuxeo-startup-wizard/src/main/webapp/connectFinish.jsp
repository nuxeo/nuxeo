<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.connectFinish" /></h1>

<%@ include file="includes/form-start.jsp" %>

 <div class="warnBlock">
  <h2> <fmt:message key="label.connectFinish.ko" /> </h2>
  <ul>
   <li><fmt:message key="label.connectFinish.ko.bad1" /></li>
   <li><fmt:message key="label.connectFinish.ko.bad2" /></li>
  </ul>
 </div>
 <p>
      <fmt:message key="label.connectFinish.ko.free" />
  </p>
</div>
<div class="buttonContainer">
<input type="button" id="btnRetry" class="glossyButton" value="<fmt:message key="label.action.retry"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<input type="submit" id="btnSkip" class="glossyButton" value="<fmt:message key="label.action.skip"/>"/>
</div>
</form>

<%@ include file="includes/footer.jsp" %>
