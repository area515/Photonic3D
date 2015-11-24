(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("Main", ['$scope', '$http', 'ModalService', function($scope, $http, ModalService) {	
		$scope.showPrintWizard = function showPrintWizard() {
		    ModalService.showModal({
		      templateUrl: "/printWizard.html",
		      controller: "printWizard",
		      inputs: {
		        title: "Print Wizard"
		      }
		    }).then(function(modal) {
		      modal.element.modal();
		      modal.close.then(function(result) {
		    	//TODO: after event logic for main screen result is from close operation!
		      });
		    });
		};
	}])
})();