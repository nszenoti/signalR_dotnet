import 'package:flutter/material.dart';
import 'dart:async';

import 'package:signalr/signalr.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _status = 'Unknown';
  FLTSignalR signalR;
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  @override
  void initState() {
    super.initState();
    initSignalRState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initSignalRState() async {
    signalR = FLTSignalR(
        hubName: 'pOSDisplayTerminalHub',
        hubToken:
        'AN:proteamin|\$ARD#JhEGpXuO42POVu6f/EgzEXn6DmlPR1gvDK5Tqr1BtEV1vMpwBFrsMhzVaWvLE3Lm9VWezqFku3mG2q3tPhQih41DMpMy8OKxCqCZ7QSHN6qdSkTaJNv2h4yTDOvBfes8BnptXCQq5+ZWsPFrUw2O1Dya0hkelsssCFsZqfeaIZXiyZyphkirZOjuP8dHzhn1yLbpmBJBRcr3v7tfBZvuN3jya+QJOOg+Encxf7qPCeEovhKcUnLSX+LPF+pI4lutB2hyNcVi4ahJhg==',
        signalRURL: 'https://apiSrasia01.zenoti.com',
        hubObservers: ['MirrorInvoice'],
        statusCallback: _onStatusChange,
        hubCallback: _onHubCallback);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('SignalR Plugin App'),
        ),
        key: _scaffoldKey,
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text('Connection Status: $_status\n',
                  style: Theme.of(context).textTheme.headline5),
              Padding(
                padding: const EdgeInsets.only(top: 20.0),
                child: RaisedButton(
                  onPressed: () async {
                    await signalR.connectSignalR();
                  },
                  child: Text('Connect'),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 20.0),
                child: RaisedButton(
                  onPressed: () async {
                    var result = await signalR.subscribeToHubMethod('MirrorInvoice');
                    print(result);
                  },
                  child: Text('Subscribe Method'),
                ),
              ),
              Padding(
                padding: const EdgeInsets.only(top: 20.0),
                child: RaisedButton(
                  onPressed: () async {
                    var result = await signalR.invokeHubMethod('joinRegister', 'f10eae3-d93c-4a45-82a8-2faa3416b028');
                    print(result);
                  },
                  child: Text('Invoke Method'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _onStatusChange(dynamic data) {
    if (mounted) {
      var status = data == null ? 'NA' : data['status'];
      setState(() {
        _status = status;
      });
    }
  }

  void _onHubCallback(dynamic message) {
    print(message);
  }
}
