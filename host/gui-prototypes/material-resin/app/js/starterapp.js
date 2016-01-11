var app = angular
              .module('starterApp', ['ngRoute','ngMaterial', 'ngFileUpload'])
              .config(function($mdThemingProvider, $mdIconProvider,$routeProvider, $locationProvider){

                  $mdIconProvider
                      .defaultIconSet("./assets/svg/avatars.svg", 128)
                      .icon("menu"       , "./assets/svg/menu.svg"        , 24)
                      .icon("share"      , "./assets/svg/share.svg"       , 24)
                      .icon("google_plus", "./assets/svg/google_plus.svg" , 512)
                      .icon("hangouts"   , "./assets/svg/hangouts.svg"    , 512)
                      .icon("twitter"    , "./assets/svg/twitter.svg"     , 512)
                      .icon("phone"      , "./assets/svg/phone.svg"       , 512)
                      .icon("icon"      , "./assets/svg/icon.svg"       , 512)
                      .icon("info-outline", "./assets/svg/material-design-icons-master/action/svg/production/ic_info_outline_48px.svg", 512)
                      .icon("settings", "./assets/svg/material-design-icons-master/action/svg/production/ic_settings_48px.svg", 512)
                      .icon("controls", "./assets/svg/material-design-icons-master/hardware/svg/production/ic_gamepad_48px.svg", 512)
                      .icon("jobs", "./assets/svg/material-design-icons-master/file/svg/production/ic_folder_48px.svg", 512)
                      .icon("add", "./assets/svg/material-design-icons-master/content/svg/production/ic_add_48px.svg", 512)
                      .icon("remove", "./assets/svg/material-design-icons-master/content/svg/production/ic_remove_48px.svg", 512)
                      .icon("send", "./assets/svg/material-design-icons-master/content/svg/production/ic_send_48px.svg", 512)
                      .icon("arrow", "./assets/svg/material-design-icons-master/content/svg/production/ic_forward_48px.svg", 512)
                      .icon("save", "./assets/svg/material-design-icons-master/content/svg/production/ic_save_48px.svg", 512)
                      .icon("delete", "./assets/svg/material-design-icons-master/action/svg/production/ic_delete_48px.svg", 512)
                      .icon("importexport", "./assets/svg/material-design-icons-master/communication/svg/production/ic_import_export_48px.svg", 512)
                      .icon("refresh", "./assets/svg/material-design-icons-master/navigation/svg/production/ic_refresh_48px.svg", 512)
                      .icon("usb", "./assets/svg/material-design-icons-master/device/svg/production/ic_usb_48px.svg", 512);

                  $routeProvider.when('/printer',
                  {
                    templateUrl:    'views/printer/printer.html',
                    controller:     'PrinterCtrl'
                  })
                  .when('/jobs',
                  {
                    templateUrl:    'views/jobs/jobs.html',
                    controller:     'JobsCtrl'
                  })
                  .when('/controls',
                  {
                    templateUrl:    'views/controls/controls.html',
                    controller:     'ControlsCtrl'
                  })
                  .when('/settings',
                  {
                    templateUrl:    'views/settings/settings.html',
                    controller:     'SettingsCtrl'
                  });

              });


app.controller('NavCtrl', 
['$scope', '$location', '$mdSidenav', function ($scope, $location,$mdSidenav) {  
  var self = this;
  console.log('hello');
  // alert("inside the nav controller")

  $scope.toggleList = function() {
    $mdSidenav('left').toggle();
  }

}]);

app.factory('SharedService', function() {
  return {
    sharedObject: {
      currentSlice : 'views/printer/testImage.PNG',
      progress: '25/258',
      cost : '$2.23',
      connection: 'UnknownC',
      status : 'Unknown',
      id : "none"
    }
  };
});

// function getId($scope, $http, SharedService, $location) {
//     $http.get('http://api.github.com/users/ergobot').
//         success(function(data) {
//             alert(JSON.stringify);
//             alert($location.path());
//             alert($location.protocol()+ "://" + $location.host() +":"+ $location.port());  
//             SharedService.sharedObject.id = data.id;
//             alert(SharedService.sharedObject.id);
//             $scope.id = SharedService.sharedObject.id;
//         });
// }

// function echo($scope, $http, SharedService, $location) {
//     var baseservice = "services";
//     var machineservice = "machine";
//     var method = "echo";
//     var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;
//     alert(fullurl);
//     $http.get(fullurl).
//         success(function(data) {
//             alert(JSON.stringify);
//             alert(data);
//             alert($location.path());
//             alert($location.protocol()+ "://" + $location.host() +":"+ $location.port());  
//             SharedService.sharedObject.id = data;
//             alert(SharedService.sharedObject.id);
//             $scope.id = SharedService.sharedObject.id;
//         });
// }

app.controller('PrinterCtrl', function($scope, $mdSidenav, $compile, SharedService, $http, $location) {
  console.log('inside printer controller');
  // alert('inside the printer controller')
  // $mdSidenav('left').close();
  //_.extend($scope, SharedService);
  refreshPrinters($scope, $http, SharedService, $location);

  $scope.imagePath = 'views/printer/testImage.PNG';
  $scope.altImagePath = 'views/printer/blank.png';
  $scope.progress = SharedService.progress;
  $scope.cost = '$2.23';
  $scope.connection = SharedService.sharedObject.connection;
  $scope.status = SharedService.sharedObject.status;

  $scope.selectedprinter = "hello";
   $scope.model = SharedService.sharedObject;

});




app.directive('info', function($rootScope) {
  return {
    scope : {
      data : '=',
      printers: '='
    },
    templateUrl: 'views/printer/tabs/info.html'
  };

});

app.directive('box', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/controls/box/box.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('motorbox', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/controls/box/motorbox.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('switchbox', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/controls/box/switchbox.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('gcodebox', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/controls/box/gcodebox.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('configuration', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/settings/printer/cards/configuration.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('serialports', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/settings/printer/cards/serialports.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('machine', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/settings/printer/cards/machine.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('projector', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/settings/printer/cards/projector.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});

app.directive('slicerconfiguration', function($rootScope) {
  return {
           scope: {
            data: '='
        },
    templateUrl: 'views/settings/printer/cards/slicerconfiguration.html',
    link : function (scope, element, attrs){
      scope.data = {
        title : attrs.ngtitle,
        label : attrs.nglabel
      }
    }
  };

});


// app.controller('BoxCtrl', function($scope, $mdSidenav, $compile, $attrs) {
//   console.log('inside box controller');
//   // console.log($attrs.title);
//   $scope.title = $attrs.title;
//   $scope.label = $attrs.label;
//   console.log('my title: :' + $scope.title);
//   // $mdSidenav('left').close()
// });



// app.factory('SharedService', function($rootScope) {
  

//   function SharedService(){
//     this.value = 'testing'

//   };
//   return $rootScope.SharedService = new SharedService();
// });

// app.directive('job',[function(){
//   return{
//     restrict: 'E',
//     scope: {
//       value: '='
//     },
//     template: 'views/printer/tabs/job.html'
//   }
// }]);
app.directive('jobprogress', function($rootScope) {
  return {
    scope : {
      data : '=',
      printers: '=',
      imagePath: '=',
      altImagePath: '=',
      progress: '=',
      cost: '='
    }
    // link: function (scope, element, attrs){
    //   scope.imagePath;
    // },
    ,
    templateUrl: 'views/printer/tabs/jobprogress.html'
  };

});

function refreshFiles($scope, $http, SharedService, $location) {
    var baseservice = "services";
    var machineservice = "files";
    var method = "list";
    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;
    $http.get(fullurl).
        success(function(data) {
            $scope.files = data;
        });
}

function refreshJobStatus($scope, $http, SharedService, $location){

    var baseservice = "services";
    var machineservice = "files";
    var method = "list";
    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;

    $http.get(fullurl).
        success(function(data) {
            $scope.files = data;
      });
}

function getPrinterByName($scope, $http, SharedService, $location, printername) {
    var baseservice = "services";
    var machineservice = "printers";
    var method = "get";

    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method +"/"+ encodeURIComponent(printername);
    console.log(fullurl);
    $http.get(fullurl).
        success(function(data) {
            $scope.printer = data;
        });
}

function refreshPrinters($scope, $http, SharedService, $location) {
    var baseservice = "services";
    var machineservice = "printers";
    var method = "list";
    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;
    $http.get(fullurl).
        success(function(data) {
            $scope.printers = data;
            $scope.printername = data[0].configuration.name;
            $scope.displays.push(printer.configuration.MonitorDriverConfig.OSMonitorID);
            return "ok";
        });

}

function refreshPrintersConfig($scope, $http, SharedService, $location) {
    var baseservice = "services";
    var machineservice = "printers";
    var method = "list";
    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;
    $http.get(fullurl).
        success(function(data) {
            $scope.printers = data;
            $scope.printername = data[0].configuration.name;
            $scope.displays.push(printer.configuration.MonitorDriverConfig.OSMonitorID);
            refreshDisplays($scope, $http, SharedService, $location);
            refreshSerialPorts($scope, $http, SharedService, $location);
        });

}

function refreshDisplays($scope, $http, SharedService, $location) {
    var baseservice = "services";
    var machineservice = "machine";
    var method = "graphicsDisplays/list";
    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;

    $http({
      method: 'GET',
      url: fullurl
        }).then(
        function successCallback(response) {
          // this callback will be called asynchronously
          // when the response is available
          // alert("refresh displays success");
          $scope.displays = data;
          $scope.displays.push(printer.configuration.MonitorDriverConfig.OSMonitorID);
        }, 
        function errorCallback(response) {
          // called asynchronously if an error occurs
          // or server returns response with an error status.
          alert("refresh displays error");
          $scope.displays = printer.configuration.MonitorDriverConfig.OSMonitorID;
        });
}

function refreshSerialPorts($scope, $http, SharedService, $location) {
    var baseservice = "services";
    var machineservice = "machine";
    var method = "serialPorts/list";
    var fullurl = $location.protocol()+ "://" + $location.host() +":"+ $location.port() + "/" + baseservice + "/" + machineservice + "/" + method;

    $http({
      method: 'GET',
      url: fullurl
        }).then(
        function successCallback(response) {
          // this callback will be called asynchronously
          // when the response is available
          alert("refresh serialports success");
          $scope.serialPorts = data;
        }, 
        function errorCallback(response) {
          // called asynchronously if an error occurs
          // or server returns response with an error status.
          // alert("refresh serialports error");
          $scope.serialPorts = [];
          alert(JSON.stringify(printer));
          $scope.serialPorts.push(printer.configuration.machineConfig.MotorsDriverConfig.ComPortSettings.PortName);
          // $scope.serialPorts = printer.configuration.machineConfig.MotorsDriverConfig.ComPortSettings.PortName;
        });
}

app.controller('JobsCtrl', function($scope, $mdSidenav, $compile, Upload, $http, SharedService, $location) {
  console.log('inside jobs controller');

$scope.printername = '';

  $scope.$watch('printername', function(v){
  // $scope.id = v;

    if($scope.printername){

      getPrinterByName($scope,$http,SharedService,$location,$scope.printername);
    // alert('printername changed' + $scope.printername);
    }
  });

  // $mdSidenav('left').close()
  refreshFiles($scope, $http, SharedService, $location);
  refreshPrinters($scope, $http, SharedService, $location);
   // $scope.selectedprinter = '';
  // upload on file select or drop
    $scope.upload = function (file) {
        Upload.upload({
            url: 'upload/url',
            data: {file: file, 'username': $scope.username}
        }).then(function (resp) {
            console.log('Success ' + resp.config.data.file.name + 'uploaded. Response: ' + resp.data);
        }, function (resp) {
            console.log('Error status: ' + resp.status);
        }, function (evt) {
            var progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
            console.log('progress: ' + progressPercentage + '% ' + evt.config.data.file.name);
        });
    };




});


app.controller('ControlsCtrl', function($scope, $mdSidenav, $compile) {
  console.log('inside controls controller');

  // $scope.data = 

  // $mdSidenav('left').close()
  $scope.data = [
  {title : 'Lift Speed',  label : 'mm/m'},
  {title : 'Lift Speed',  label : 'mm/m'},
  {title : 'Lift Speed',  label : 'mm/m'}
  ];

  
  $scope.toggle = false;

  $scope.motors = function(){
    if($scope.toggle == false){
      return "Disabled";
    } else if($scope.toggle == true){
      return "Enabled";
    }
  };
  
  
});
app.controller('SettingsCtrl', function($scope, $mdSidenav, $compile, Upload, $http, SharedService, $location) {
  console.log('inside settings controller');

  $scope.printername = '';
  $scope.baudrates = [
                      "2400",
                      "4800",
                      "9600",
                      "14400",
                      "19200",
                      "28800",
                      "38400",
                      "56000",
                      "57600",
                      "115200"
                              ];

  $scope.$watch('printername', function(v){
    if($scope.printername){
      getPrinterByName($scope,$http,SharedService,$location,$scope.printername);
    }
  });

  refreshPrintersConfig($scope, $http, SharedService, $location);

});
    // .then(function(){alert("hello1")})
      // refreshDisplays($scope, $http, SharedService, $location)
    // .then(function(){alert("hello2")}
      // refreshSerialPorts($scope, $http, SharedService, $location)
    // );
   // alert('stop stop combat stop');
   // refreshDisplays($scope, $http, SharedService, $location);
   // refreshSerialPorts($scope, $http, SharedService, $location);

   

  // $mdSidenav('left').close()




/*
old stuff
// $http.get(fullurl).
    //     success(function(data) {
    //         // alert(JSON.stringify);
    //         // alert(data);
    //         // alert($location.path());
    //         // alert($location.protocol()+ "://" + $location.host() +":"+ $location.port());  
    //         $scope.displays = data;
    //         $scope.displays.push(printer.configuration.MonitorDriverConfig.OSMonitorID);
    //         // alert($scope.displays);
    //         // $scope.displayname = data[0].configuration.name;
    //         // alert(data[0].configuration.name);

    //         // alert(data.length);
    //     });
*/