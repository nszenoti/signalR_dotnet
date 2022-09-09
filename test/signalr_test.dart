import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
// import 'package:signalr/signalr.dart';

void main() {
  const channel = MethodChannel('signalr');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  // test('getPlatformVersion', () async {
  //   expect(await Signalr.platformVersion, '42');
  // });
}
