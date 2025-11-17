import 'package:flutter/services.dart';

class CliSitefEventListener {
  static const EventChannel _eventChannel = EventChannel('clisitef_events');

  Stream<Map<Object?, Object?>>? _eventStream;

  Stream<Map<Object?, Object?>> get events {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((event) => Map<String, dynamic>.from(event));
    _eventStream!.listen((event) {
      print("EVENT RECEIVED: $event");
      print("Event type: ${event['type']}");
      print("Event data: ${event['data']}");
    });
    return _eventStream!;
  }

  Stream<String> get messages {
    return events
        .where((event) => event['type'] == 'message')
        .map((event) {

      final data = event['data'] as Map<Object?, Object?>;

      print("MESSAGE: $data");

      return data['message'] as String;
    });
  }

  Stream<Map<String, dynamic>> get results {
    return events
        .where((event) => event['type'] == 'transaction_result')
        .map((event) {
      final data = event['data'] as Map<String, dynamic>;

      // ✅ Print aqui dentro do map
      print("📊 TRANSACTION RESULT: $data");
      print("   Stage: ${data['stage']}");
      print("   ResultCode: ${data['resultCode']}");
      print("   Success: ${data['success']}");

      return data;
    });
  }

  Stream<String> get errors {
    print("ERRORS: ${events
        .where((event) => event['type'] == 'error')
        .map((event) => event['data'] as String)}");

    return events
        .where((event) => event['type'] == 'error')
        .map((event) => event['data'] as String);
  }


}