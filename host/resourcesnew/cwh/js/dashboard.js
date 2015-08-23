(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("Dashboard", function ($scope, $http) {
        $http.get('/services/files/list').success(function(data) {
        	$scope.printables = data;
        });
        $http.get('/services/printers/list').success(function(data) {
        	$scope.printers = data;
        });
        $http.get('/services/settings/visibleCards').success(function(data) {
        	$scope.visibleCards = data;
        });
	})

})();