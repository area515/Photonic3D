var app = angular.module("plunker", ['ngRoute', 'angularModalService'])
  .config(function ($routeProvider, $locationProvider, $httpProvider) {

    $routeProvider.when('/overview',
    {
      templateUrl:    'views/overview/overview.html',
      controller:     'OverviewCtrl'
    })
    .when('/jobs',
    {
      templateUrl:    'views/jobs.html',
      controller:     'JobsCtrl'
    })
    .when('/motors',{
      templateUrl:    'views/motors.html',
      controller:     'MotorsCtrl'
    })
    .when('/dlp',{
      templateUrl:    'views/dlp.html',
      controller:     'DlpCtrl'
    })
    .when('/printer', {
      templateUrl:    'views/printer.html',
      controller:     'PrinterCtrl'
    });
});

app.controller('NavCtrl', 
['$scope', '$location', function ($scope, $location) {  
  $scope.navClass = function (page) {
    var currentRoute = $location.path().substring(1) || 'home';
    return page === currentRoute ? 'active' : '';
  };
}]);

app.controller('JobsCtrl', function($scope, $compile) {
  console.log('inside jobs controller');

});

app.controller('MotorsCtrl', function($scope, $compile) {
  console.log('inside motors controller');

});

app.controller('DlpCtrl', function($scope, $compile) {
  console.log('inside dlp controller');

});

app.controller('PrinterCtrl', function($scope, $compile) {
  console.log('inside printer controller');

});
app.directive('profile', function() {
  return {
    scope : {
      data : '='
    },
    templateUrl: 'profile.html'
  };

});

app.directive('printerconnectionpanel', function() {
  return {
    scope : {
      data : '='
    },
    templateUrl: 'views/overiew/panels/printerconnectionpanel.html'
  };

});
app.directive('printerinfopanel', function() {
  return {
    scope : {
      data : '='
    },
    templateUrl: 'views/overiew/panels/printerinfopanel.html'
  };

});

// Modal controllers
app.controller('YesNoController', ['$scope', 'close', function($scope, close) {

  $scope.close = function(result) {
    close(result, 500); // close, but give 500ms for bootstrap to animate
  };

}]);

app.controller('CustomController', ['$scope', 'close', function($scope, close) {

  $scope.close = close;

}]);

app.controller('ComplexController', [
  '$scope', '$element', 'title', 'close', 
  function($scope, $element, title, close) {

  $scope.name = null;
  $scope.age = null;
  $scope.title = title;
  
  //  This close function doesn't need to use jQuery or bootstrap, because
  //  the button has the 'data-dismiss' attribute.
  $scope.close = function() {
    close({
      name: $scope.name,
      age: $scope.age
    }, 500); // close, but give 500ms for bootstrap to animate
  };

  //  This cancel function must use the bootstrap, 'modal' function because
  //  the doesn't have the 'data-dismiss' attribute.
  $scope.cancel = function() {

    //  Manually hide the modal.
    $element.modal('hide');
    
    //  Now call close, returning control to the caller.
    close({
      name: $scope.name,
      age: $scope.age
    }, 500); // close, but give 500ms for bootstrap to animate
  };

}]);

// End of modal controllers







app.controller('OverviewCtrl', function($scope, ModalService, $compile, $http) {
  console.log('inside overview controller');


  $scope.user = {
    name : "Shagai",
    email: "test@gmail.com"
  }
  //https://api.github.com/users/ergobot

  var url = 'https://api.github.com/users/ergobot';
  $scope.buttonclick = function(){

    $http.get(url).success(function(data){
        $scope.output = data;
        
    });

  }

  $scope.showYesNo = function() {

    ModalService.showModal({
      templateUrl: "views/modals/yesno.html",
      controller: "YesNoController"
    }).then(function(modal) {
      modal.element.modal();
      modal.close.then(function(result) {
        $scope.yesNoResult = result ? "You said Yes" : "You said No";
      });
    });

  };

  $scope.showCustom = function() {

    ModalService.showModal({
      templateUrl: "views/modals/custom.html",
      controller: "CustomController"
    }).then(function(modal) {
      modal.close.then(function(result) {
        $scope.customResult = "All good!";
      });
    });

  };

  $scope.showComplex = function() {

    ModalService.showModal({
      templateUrl: "views/modals/complex.html",
      controller: "ComplexController",
      inputs: {
        title: "A More Complex Example"
      }
    }).then(function(modal) {
      modal.element.modal();
      modal.close.then(function(result) {
        $scope.complexResult  = "Name: " + result.name + ", age: " + result.age;
      });
    });

  };



});