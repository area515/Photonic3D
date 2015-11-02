(function() {
		var cwhApp = angular.module('cwhApp', ['ngRoute', 'angularModalService']);
		
		cwhApp.config(['$routeProvider', '$locationProvider',
		         	  function($routeProvider, $locationProvider) {
		         	    $routeProvider.when('/mainPage', {
		         	        templateUrl: '/main.html',
		         	        controller: 'Main',
		         	        controllerAs: 'main'
		         	    })
		         	    
		         	    $routeProvider.otherwise({
		         	    	redirectTo: '/mainPage'
		         	    });
		         	    
		         	    $locationProvider.html5Mode({enabled: true, requireBase: true, rewriteLinks: true});
		         	}])//*/
		
		cwhApp.controller('IndexController', ['$scope', '$http', 'ModalService', function($scope, $http, ModalService) {			  
    		this.changeCurrentPage = function (newPageName) {
    			this.currentPage = newPageName;
    		}
    		
			$scope.$on("MachineResponse", function (event, args) {
				//args = machineResponse, successFunction, afterErrorFunction
	        	if (!args.machineResponse.response) {
	        		args.machineResponse.command = "Error On " + args.machineResponse.command;
	        		$scope.currentError = args.machineResponse;
	            	
	            	$('#errorModal').modal().after
	            	if (args.afterErrorFunction != null) {
	            		args.afterErrorFunction(args.machineResponse);
	            	}
	        	} else if (args.successFunction != null) {
	        		args.successFunction(args.machineResponse);
	        	}
	        });
			$scope.$on("HTTPError", function (event, args){
				//args = data, status, headers, config, statusText
	    		var customMessage;
				if (args.status == "401") {
					customMessage = "You logged in wrong.";
				} else if (args.status == "400") {
					customMessage = args.statusText;
				} else if (args.startText == null) {
					customMessage = "Problem communicating with host printer.";
				} else {
					customMessage = "Problem communicating with host printer. (http:" + args.statusText + ")";
				}
				
				$scope.currentError = {command:"Server Error " + args.status, message : customMessage};
		    	$('#errorModal').modal();
		    });
	        $http.get('/services/settings/visibleCards').success(function(data) {
	        	$scope.visibleCards = data;
	        });
	        $http.get('/services/settings/integerVersion').success(function(data) {
	        	$scope.integerVersion = data;
	        });
	        this.currentPage = 'mainPage';
	}])//*/
	
    bootcards.init( {
        offCanvasHideOnMainClick : true,
        offCanvasBackdrop : true,
        enableTabletPortraitMode : true,
        disableRubberBanding : true,
        disableBreakoutSelector : 'a.no-break-out'
      });
    
})();