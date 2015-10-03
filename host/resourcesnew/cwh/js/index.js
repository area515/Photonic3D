(function() {
		var cwhApp = angular.module('cwhApp', ['ngRoute']);
		cwhApp.config(['$routeProvider', '$locationProvider',
    	  function($routeProvider, $locationProvider) {
    	    $routeProvider.when('/dashboardPage', {
    	        templateUrl: '/dashboard.html',
    	        controller: 'Dashboard',
    	        controllerAs: 'dashboard'
    	    })
    	    $routeProvider.when('/printersPage', {
    	        templateUrl: '/printers.html',
    	        controller: 'PrintersController',
    	        controllerAs: 'printersController'
    	    })
    	    $routeProvider.when('/printerControlsPage', {
    	        templateUrl: '/printerControls.html',
    	        controller: 'PrinterControlsController',
    	        controllerAs: 'printerControlsController'
    	    })
    	    $routeProvider.otherwise({
    	    	redirectTo: '/dashboardPage'
    	    });
    	    
    	    $locationProvider.html5Mode({enabled: true, requireBase: true, rewriteLinks: true});
    	}])//*/
    	
    	cwhApp.controller("IndexController", function ($scope, $http) {
    		this.changeCurrentPage = function changeCurrentPage(newPageName) {
    			this.currentPage = newPageName;
    		}
    		
    		this.executeDiagnostic = function executeDiagnostic() {
    	        $http.get(service + printerName).success(
    	        		function (data) {
    	        			$scope.$emit("MachineResponse", {machineResponse: {command:"Executed Diagnostic", message:"Successfully executed diagnostic"}, successFunction:null, afterErrorFunction:null});
    	        		}).error(
        				function (data, status, headers, config, statusText) {
     	        			$scope.$emit("HTTPError", {status:status, statusText:data});
    	        		})
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
	        this.currentPage = 'dashboard';
	        
    	})
    	
	    bootcards.init( {
	        offCanvasHideOnMainClick : true,
	        offCanvasBackdrop : true,
	        enableTabletPortraitMode : true,
	        disableRubberBanding : true,
	        disableBreakoutSelector : 'a.no-break-out'
	      });
	    
    })();