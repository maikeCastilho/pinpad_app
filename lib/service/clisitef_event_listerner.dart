import 'package:flutter/services.dart';
import 'dart:async';

class CliSiTefEventListener {
  static const EventChannel _eventChannel = EventChannel('clisitef_events');
  Stream<Map<String, dynamic>>? _eventStream;

  Stream<Map<String, dynamic>> get events {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((event) {
      if (event is Map) {
        final mapped = Map<String, dynamic>.from(event.map(
              (key, value) => MapEntry(
            key.toString(),
            value is Map ? Map<String, dynamic>.from(value) : value,
          ),
        ));

        // ✅ Print do mapa bruto completo
        print("🔵 MAPA BRUTO: $mapped");

        return mapped;
      }
      return <String, dynamic>{};
    });

    return _eventStream!;
  }


  Stream<String> get messages {
    return events.where((event) => event['type'] == 'message').map((event) {
      final data = event['data'];

      if (data is Map) {
        final message = data['message'];
        print("📝 MESSAGE: $message");
        return message?.toString() ?? '';
      }

      return '';
    });
  }

  Stream<Map<String, dynamic>> get results {
    return events
        .where((event) => event['type'] == 'transaction_result')
        .map((event) {
      final data = event['data'];

      // ✅ Converter data para Map<String, dynamic>
      if (data is Map) {
        final resultMap = Map<String, dynamic>.from(data.map(
              (key, value) => MapEntry(key.toString(), value),
        ));

        // print("════════════════════════════════════════");
        // print("📊 TRANSACTION RESULT:");
        // print("   Stage: ${resultMap['stage']}");
        // print("   Code: ${resultMap['resultCode']}");
        // print("   Success: ${resultMap['success']}");
        // print("════════════════════════════════════════");

        return resultMap;
      }

      return <String, dynamic>{};
    });
  }

  Stream<String> get errors {
    return events
        .where((event) => event['type'] == 'error')
        .map((event) {
      final data = event['data'];
      final error = data?.toString() ?? 'Erro desconhecido';
      print("❌ ERROR: $error");
      return error;
    });
  }
}
