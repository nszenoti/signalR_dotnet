import 'dart:async';

import 'package:flutter/services.dart';
import 'package:pedantic/pedantic.dart';
import './Constants.dart';

class FLTSignalR {
  static const MethodChannel _channel =
      MethodChannel('com.zenoti.mpos/signalr');
  final String? hubToken;
  final String? signalRURL;
  final String? hubName;
  final List<String>? hubObservers;

  final Function(dynamic)? statusCallback;
  final Function(dynamic)? hubCallback;

  FLTSignalR(
      {this.hubName,
      this.hubToken,
      this.signalRURL,
      this.hubObservers,
      this.statusCallback,
      this.hubCallback});

  Future<bool?> connectSignalR() async {
    var args = {
      HubArgument.hubToken: hubToken,
      HubArgument.signalRURL: signalRURL,
      HubArgument.hubName: hubName,
      HubArgument.hubObservers: hubObservers
    };
    _handleCallbacks();
    final status =
        await _channel.invokeMethod<bool>(InvokeMethod.createAndConnect, args);
    return status;
  }

  Future<bool?> subscribeToHubMethod(String name) async {
    var args = {
      HubArgument.methodName: name,
    };
    var status = await _channel.invokeMethod(InvokeMethod.subscribeToHub, args);
    return status;
  }

  Future<void> disconnect() async {
    unawaited(_channel.invokeMethod(InvokeMethod.disconnect));
  }

  Future<void> dispose() async {
    unawaited(_channel.invokeMethod(InvokeMethod.dispose));
  }

  Future<dynamic> invokeHubMethod(String name, dynamic arguments) async {
    var args = {
      HubArgument.methodName: name,
      HubArgument.args: arguments,
    };
    final result =
        await _channel.invokeMethod(InvokeMethod.invokeHubMethod, args);
    return result;
  }

  void _handleCallbacks() {
    _channel.setMethodCallHandler((call) {
      switch (call.method) {
        case CallbackMethod.connectionStatus:
          statusCallback!(call.arguments);
          break;
        case CallbackMethod.onEvent:
          hubCallback!(call.arguments);
          break;
      }
      return Future<void>.value();
    });
  }
}
