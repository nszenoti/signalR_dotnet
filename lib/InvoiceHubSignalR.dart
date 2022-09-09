import 'package:flutter/services.dart';

import 'Constants.dart';

abstract class InvoiceHubDelegate {
  void invoiceOpened(Map<String, dynamic> args);
  void connectionStatus(int status);
}

class InvoiceHubSignalR {
  static const MethodChannel _channel =
      MethodChannel('com.zenoti.mpos/flutter_invoice_signalR');
  final String? hubToken;
  final String? signalRURL;
  final String? hubName;
  final InvoiceHubDelegate? delegate;
  // final List<String> hubObservers;

  InvoiceHubSignalR({
    this.hubName,
    this.hubToken,
    this.signalRURL,
    this.delegate,
  }) {
    _channel.setMethodCallHandler((call) {
      _handleCallbacks(call);
      return Future<void>.value();
    });
  }

  void connectToInvoiceHub() {
    var args = {
      HubArgument.hubToken: hubToken,
      HubArgument.signalRURL: signalRURL,
      HubArgument.hubName: hubName,
    };
    _channel.invokeMethod('StartInvoiceHubConnection',args);
  }

  void disconnectFromInvoiceHub() {
    _channel.invokeMethod('DisconnectInvoiceHubConnection');
  }

  void joinInvoice(String invoiceId) {
    var args = <String, dynamic>{
      'InvoiceId': invoiceId,
    };
    _channel.invokeMethod('JoinInvoice', args);
  }

  void leaveInvoice(String invoiceId) {
    var args = <String, dynamic>{
      'InvoiceId': invoiceId,
    };
    _channel.invokeMethod('LeaveConnect', args);
  }

  void resetConnection() {
    _channel.invokeMethod('resetConnection');
  }

  void notifyInvoiceOpen(String invoiceId, String userName) {
    var args = <String, dynamic>{'InvoiceId': invoiceId, 'UserName': userName};
    _channel.invokeMethod('NotifyInvoiceOpen', args);
  }

  void _handleCallbacks(MethodCall call) {
    switch (call.method) {
      case 'NotifyInvoiceOpen':
        delegate?.invoiceOpened(Map<String, dynamic>.from(call.arguments));
        break;
      case 'connection_result':
        if (call.arguments != null) {
          var args = Map<String, dynamic>.from(call.arguments);
          dynamic status = args['connectionStatus'];
          if (status is int) {
            delegate!.connectionStatus(status);
          }
        }
        break;
      default:
        return;
    }
  }
}
