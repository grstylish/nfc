var exec = require('cordova/exec');

exports.scan = function (success, error) {
  exec(success, error, "NfcPlugin", "scan", []);
};

exports.showSettings = function (success, error) {
  exec(success, error, "NfcPlugin", "showSettings", []);
};

exports.enabled = function (success, error) {
  exec(success, error, "NfcPlugin", "enabled", []);
};
