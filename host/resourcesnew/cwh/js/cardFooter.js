(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("CardFooter", function ($scope, $http) {
		this.executeDiagnostic = function executeDiagnostic() {
	        $http.get("services/machine/executeDiagnostic").success(
	        		function (data) {
	        			$scope.$emit("MachineResponse", {machineResponse: {command:"Executed Diagnostic", message:"Successfully executed diagnostic"}, successFunction:null, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
		}
	})

})();