(function() {
		var cwhApp = angular.module('cwhApp', ['ui.bootstrap', 'ngRoute', 'cwh.comport', 'cwh.spinner', 'cwh.webSocket', 'cwh.testscript', 'ngFileUpload', 'ngAnimate', 'chart.js', 'printJobModelViewer']);
		var firstCacheId = new Date().toDateString();
		cwhApp.filter('secondsToDateTime', [function() {
		    return function(milliseconds) {
		        return new Date(1970, 0, 1).setMilliseconds(milliseconds);
		    };
		}]);
		
		cwhApp.directive('onEnter', function () {
		    return function (scope, element, attrs) {
		        element.bind("keydown keypress", function (event) {
		            if(event.which === 13) {
		                scope.$apply(function (){
		                    scope.$eval(attrs.onEnter);
		                });

		                event.preventDefault();
		            }
		        });
		    };
		});
		
		cwhApp.factory('photonicUtils', ['$http', '$rootScope', function($http, $rootScope) {
	        return {
	        	previewExternalStateId:firstCacheId,
	        	
	        	testScript: function(controller, scriptName, returnType, script, successFunction) {
	    			var printerNameEn = encodeURIComponent(controller.currentPrinter.configuration.name);
	    			var scriptNameEn = encodeURIComponent(scriptName);
	    			var returnTypeEn = encodeURIComponent(returnType);
	    			
	    			$http.post('/services/printers/testScript/' + printerNameEn + "/" + scriptNameEn + "/" + returnTypeEn, script).success(function (data) {
	    				controller.graph = data.result;
	    				if (data.error) {
	    					$rootScope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:data.errorDescription}, successFunction:null, afterErrorFunction:null});
	    	     			return;
	    	     		} else if (returnType.indexOf("[") > -1){
	    					$('#graphScript').modal();
	    				} else {
	    					$rootScope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:"Successful execution. Script returned:" + JSON.stringify(data.result), response:true}, successFunction:null, afterErrorFunction:null});
	    				}
	    				if (successFunction != null) {
	    					successFunction();
	    				}
	    			}).error(function (data, status, headers, config, statusText) {
	    				$rootScope.$emit("HTTPError", {status:status, statusText:data});
	        		})
	    		},
	    		
	    		getPrintFileProcessorIconClass: function getPrintFileProcessorIconClass(processorContainer) {
	    			if (processorContainer == null || processorContainer.printFileProcessor == null) {
	    				return "Unknown";
	    			}
	    			if (processorContainer.printFileProcessor.friendlyName === 'Image') {
	    				return "fa-photo";
	    			}
	    			if (processorContainer.printFileProcessor.friendlyName === 'Maze Cube') {
	    				return "fa-cube";
	    			}			
	    			if (processorContainer.printFileProcessor.friendlyName === 'STL 3D Model') {
	    				return "fa-object-ungroup";
	    			}			
	    			if (processorContainer.printFileProcessor.friendlyName === 'Creation Workshop Scene') {
	    				return "fa-diamond";
	    			}
	    			if (processorContainer.printFileProcessor.friendlyName === 'Zip of Slice Images') {
	    				return "fa-stack-overflow";
	    			}
	    			if (processorContainer.printFileProcessor.friendlyName === 'Simple Text') {
	    				return "fa-bold";
	    			}
	    			if (processorContainer.printFileProcessor.friendlyName === 'Scalable Vector Graphics') {
	    				return "fa-puzzle-piece";
	    			}
	    			if (processorContainer.printFileProcessor.friendlyName === 'Coin') {
	    				return "fa-user-circle";
	    			}
	    			return "fa-question-circle";
	    		},

	            clearPreviewExternalState: function() {
	        		this.previewExternalStateId = Math.random();
	            }
	        };
	    }]);

		cwhApp.config(['$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {
		    if (!$httpProvider.defaults.headers.get) {
		        $httpProvider.defaults.headers.get = {};    
		    }    

		    //disable IE ajax request caching
		    $httpProvider.defaults.headers.get['If-Modified-Since'] = 'Mon, 26 Jul 1997 05:00:00 GMT';
		    
		    //TODO: We should be removing these ONLY on CORS requests.  Other requests should keep them.
		    //$httpProvider.defaults.headers.get['Cache-Control'] = 'no-cache';
		    //$httpProvider.defaults.headers.get['Pragma'] = 'no-cache';

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
    	    $routeProvider.when('/usersPage', {
    	        templateUrl: '/users.html',
    	        controller: 'UsersController',
    	        controllerAs: 'usersController'
    	    })
    	    $routeProvider.otherwise({
    	    	redirectTo: '/dashboardPage'
    	    });
    	    
    	    $locationProvider.html5Mode({enabled: true, requireBase: true, rewriteLinks: true});
    	}])//*/
    	
    	cwhApp.controller("IndexController", function ($rootScope, $scope, $http) {
    		this.changeCurrentPage = function changeCurrentPage(newPageName) {
    			this.currentPage = newPageName;
    		}
    		
			$rootScope.$on("MachineResponse", function (event, args) {
				//args = machineResponse, successFunction, afterErrorFunction
            	if (!args.machineResponse.response) {
            		args.machineResponse.command = "Error On " + args.machineResponse.command;
            		$rootScope.currentError = args.machineResponse;
                	
                	$('#errorModal').modal().after
                	if (args.afterErrorFunction != null) {
                		args.afterErrorFunction(args.machineResponse);
                	}
            	} else if (args.successFunction != null) {
            		args.successFunction(args.machineResponse);
            	} else {
            		$rootScope.currentError = args.machineResponse;
                	
                	$('#errorModal').modal().after
                	if (args.afterErrorFunction != null) {
                		args.afterErrorFunction(args.machineResponse);
                	}
            	}
            });
			$rootScope.$on("HTTPError", function (event, args){
				//args = data, status, headers, config, statusText
        		var customMessage;
    			if (args.status == "401") {
    				customMessage = "You logged in wrong.";
    			} else if (args.status == "400") {
    				customMessage = args.statusText;
    				args.status = "";
    			} else if (args.statusText == null) {
    				customMessage = "Problem communicating with host printer.";
    			} else {
    				customMessage = "Problem communicating with host printer. (http:" + args.statusText + ")";
    			}
    			
    			$rootScope.currentError = {command:"Server Error " + args.status, message : customMessage};
    	    	$('#errorModal').modal();
    	    });

			$http.get('/services/settings/printerProfileRepo').success(function(data) {
				$scope.repo = data;
			});
	        $http.get('/services/settings/visibleCards').success(function(data) {
            	$scope.visibleCards = data;
            });
			$http.get('/services/settings/releaseTagName').success(function(data) {
				$scope.releaseTagName = data;
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