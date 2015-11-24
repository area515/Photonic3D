(function() {
	var cwhApp = angular.module('cwhApp');
	
	cwhApp.controller('printWizard', ['$scope', '$element', 'title', 'close', function($scope, $element, title, close) {
	  $scope.title = title;
	  $scope.page = "choosePrintable";
	  
	  $scope.close = function() {
		    /*close({
		    	//TODO: This is the close data structure to return a value
		    }, 500); // close, but give 500ms for bootstrap to animate*/
	  };
	
	  $scope.cancel = function() {
	    $element.modal('hide');
	    
	    
	    /*close({
	    	//TODO: This is the close data structure to return a value
	    }, 500); // close, but give 500ms for bootstrap to animate*/
	  };
	  
	  $scope.nextPage = function nextPage() {
		  if ($scope.page == "choosePrintable") {
			  $scope.page = "removeVat";
			  //TODO: Do printable chosen code!
		  } else if ($scope.page == "removeVat") {
			  $scope.page = "calibrate";
			  //TODO: Do removeVat code!
		  } else if ($scope.page == "calibrate") {
			  $scope.page = "insertResinVat";
			  //TODO: Do calibrate code
		  } else if ($scope.page == "insertResinVat") {
			  $scope.page = "readyToPrint"
			  //TODO: Start insertResinVat code
		  } else if ($scope.page == "readyToPrint") {
			  $scope.cancel();
			  //TODO: Start print!!
		  }
	  };
	}]);//*/
})();