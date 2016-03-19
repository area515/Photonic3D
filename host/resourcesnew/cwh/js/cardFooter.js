(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("CardFooter", function ($scope, $http, $window) {
		$scope.executeDiagnostic = function executeDiagnostic() {
	        $http.get("services/machine/executeDiagnostic").success(
	        		function (data) {
	        			$scope.$emit("MachineResponse", {machineResponse: {command:"Executed Diagnostic", message:"An email with diagnostic support information has been sent to your configured support email contact.", response:true}, successFunction:null, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
		}
		
		$scope.downloadDiagnostic = function downloadDiagnostic() {
        	$window.location.href = "services/machine/downloadDiagnostic/LogBundle.zip";
        }
	})

})();