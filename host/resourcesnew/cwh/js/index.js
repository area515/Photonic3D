(function() {
		var cwhApp = angular.module('cwhApp', ['ui.bootstrap', 'ngRoute', 'cwh.comport', 'cwh.spinner', 'cwh.webSocket', 'cwh.testscript', 'ngFileUpload', 'ngAnimate', 'chart.js']);
		cwhApp.filter('secondsToDateTime', [function() {
		    return function(milliseconds) {
		        return new Date(1970, 0, 1).setMilliseconds(milliseconds);
		    };
		}]);
		cwhApp.config(['$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {
		    if (!$httpProvider.defaults.headers.get) {
		        $httpProvider.defaults.headers.get = {};    
		    }    

		    //disable IE ajax request caching
		    $httpProvider.defaults.headers.get['If-Modified-Since'] = 'Mon, 26 Jul 1997 05:00:00 GMT';
		    $httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
		    $httpProvider.defaults.headers.get['Pragma'] = 'no-cache';

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
    	    $routeProvider.when('/printablesPage', {
    	        templateUrl: '/printables.html',
    	        controller: 'PrintablesController',
    	        controllerAs: 'printablesController'
    	    })
    	    $routeProvider.when('/printJobsPage', {
    	        templateUrl: '/printJobs.html',
    	        controller: 'PrintJobsController',
    	        controllerAs: 'printJobsController'
    	    })
    	    $routeProvider.when('/settingsPage', {
    	        templateUrl: '/settings.html',
    	        controller: 'SettingsController',
    	        controllerAs: 'settingsController'
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
            	} else {
            		$scope.currentError = args.machineResponse;
                	
                	$('#errorModal').modal().after
                	if (args.afterErrorFunction != null) {
                		args.afterErrorFunction(args.machineResponse);
                	}
            	}
            });
			$scope.$on("HTTPError", function (event, args){
				//args = data, status, headers, config, statusText
        		var customMessage;
    			if (args.status == "401") {
    				customMessage = "You logged in wrong.";
    			} else if (args.status == "400") {
    				customMessage = args.statusText;
    				args.status = "";
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