import 'package:flutter/services.dart';

class SiTefService {
  static const platform = MethodChannel('clisitef_channel');


  Future<bool> configurarSitef({
    required String ip,
    required String loja,
    required String terminal,
  }) async {
    try {
      await platform.invokeMethod('configurarSitef', {
        'ip': ip,
        'loja': loja,
        'terminal': terminal,
      });

      return true;

    } catch (e) {
      print("Erro ao configurar: $e");
      return false;
    }
  }

  Future<void> enviarTrace() async {
    try {
      await platform.invokeMethod('enviarTrace');
    } catch (e) {
      print("Erro ao enviar Trace: $e");
    }
  }

  Future<String?> iniciarPagamento({required String valor, String modalidade = "0"}) async {
    try{
      final String result = await platform.invokeMethod(
          'iniciarPagamento',
          {
            'valor': valor,
            'modalidade': modalidade
          },
      );
      print("Resultado do Payment: $result");
      return result;
    } on PlatformException catch (e) {
      return e.message;
    }
  }

  Future<String?> verificarPendencias() async {
    try {
      final String result = await platform.invokeMethod('verificarPendencias');
      return result;
    } catch (e) {
      print("Erro ao verificar pendências: $e");
      return e.toString();
    }
  }

  Future<String?> cancelarTransacao() async {
    try {
      final String result = await platform.invokeMethod('cancelarTransacao');
      return result;
    } catch (e) {
      print("Erro ao verificar pendências: $e");
      return e.toString();
    }
  }

  // 🔧 Menu Administrativo
  Future<void> abrirAdmin() async {
    try {
      await platform.invokeMethod('abrirAdmin');
    } catch (e) {
      print("Erro ao abrir admin: $e");
    }
  }

  Future<void> testarComunicacao() async {
    try{
      await platform.invokeMethod('testarConexao');
    } catch (e) {
      print("Erro ao testar a comunicacao");
    }
  }
}