function invokeGetQuotaStatistics(currentDocId, currentLang, displayMessage) {
	var ctx = {
	};

	var getQuotasExec = jQuery().automation('Quotas.GetStatistics');
	getQuotasExec.setContext(ctx);
	getQuotasExec.addParameter("documentRef", currentDocId);
	getQuotasExec.addParameter("language", currentLang);
	getQuotasExec.executeGetBlob(
     function(data, status, xhr) {
		jQuery.plot(jQuery("#quota_stats"), data, {series: {pie: {show: true,label: {show: false}}}});
	}, function(xhr, status, errorMessage) {
		jQuery('<div>'+displayMessage+'</div>').appendTo('#quota_stats');
	}, true);
};

function displayQuotaStats(currentDocId, currentLang, displayMessage) {
	invokeGetQuotaStatistics(currentDocId, currentLang, displayMessage);
};