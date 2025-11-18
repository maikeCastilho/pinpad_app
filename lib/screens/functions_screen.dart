import 'package:flutter/material.dart';
import '../service/clisitef.dart';
import '../service/clisitef_event_listerner.dart';
import '../widgets/card.dart';

class FunctionsScreen extends StatefulWidget {
  const FunctionsScreen({super.key});

  @override
  State<FunctionsScreen> createState() => _FunctionsScreenState();
}

class _FunctionsScreenState extends State<FunctionsScreen> {
  final SiTefService _sitef = SiTefService();
  final CliSiTefEventListener _eventListener = CliSiTefEventListener();

  String _currentMessage = '';
  Map<String, dynamic> _currentResult = {};
  String _currentError = '';
  bool _isLoading = false;
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _listenToEvents();
  }

  void _listenToEvents() {
    _eventListener.messages.listen((message) {
      setState(() {
        _currentMessage = message;
        _isProcessing = true;
      });
    });

    _eventListener.results.listen((result) {
      final data = result['data'] as Map<String, dynamic>;
      final success = data['success'] as bool;

      setState(() {
        _isProcessing = false;
        _currentResult = data;
        _currentMessage = '';
      });
    });

    // ✅ Escutar eventos gerais
    _eventListener.events.listen((event) {
      final type = event['type'] as String?;
      final data = event['data']; // ✅ Pegar data sem cast ainda

      if (type == 'menu_required' && data != null) {
        _showMenuDialog(data as Map<String, dynamic>); // ✅ Cast aqui
      }

      if (type == 'confirmation_required' && data != null) {
        _showConfirmationDialog(data as Map<String, dynamic>); // ✅ Cast aqui
      }

      if (type == 'clear_message') {
        setState(() {
          _currentMessage = '';
        });
      }
    });
  }

  void _showMenuDialog(Map<String, dynamic> data) {
    final title = data['title'] as String? ?? 'Selecione';
    final options = data['options'] as List<dynamic>? ?? [];

    showDialog(
      context: context,
      barrierDismissible: false,
      useRootNavigator: true, // ✅ IMPORTANTE: Usar navigator raiz
      builder: (BuildContext dialogContext) => WillPopScope(
        onWillPop: () async => false,
        child: AlertDialog(
          title: Text(title),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: options.map((option) {
                final optionStr = option.toString();
                final parts = optionStr.split(':');
                final code = parts[0].trim();
                final description = parts.length > 1 ? parts[1].trim() : optionStr;

                return Card(
                  margin: const EdgeInsets.symmetric(vertical: 4),
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    title: Text(
                      description,
                      style: const TextStyle(fontSize: 16),
                    ),
                    trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                    onTap: () {
                      Navigator.of(dialogContext).pop();
                      _sitef.sendMenuSelection(code);
                    },
                  ),
                );
              }).toList(),
            ),
          ),
        ),
      ),
    );
  }

// ✅ Enviar seleção de menu
  void _sendMenuSelection(String selectedOption) {
    _sitef.sendMenuSelection(selectedOption);
  }

// ✅ Mostrar dialog de confirmação
  void _showConfirmationDialog(Map<String, dynamic> data) {
    final message = data['message'] as String? ?? 'Confirmar operação?';

    // ✅ Usar o context da tela atual (não da Activity)
    showDialog(
      context: context,
      barrierDismissible: false,
      useRootNavigator: true, // ✅ IMPORTANTE: Usar navigator raiz
      builder: (BuildContext dialogContext) => WillPopScope(
        onWillPop: () async => false, // ✅ Bloquear botão voltar
        child: AlertDialog(
          title: const Text('Confirmação'),
          content: Text(message),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                _sitef.sendConfirmation(false);
              },
              child: const Text(
                'Não',
                style: TextStyle(fontSize: 16),
              ),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                _sitef.sendConfirmation(true);
              },
              child: const Text(
                'Sim',
                style: TextStyle(fontSize: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _executarFuncao(String nome, Future<String?> Function() funcao) async {
    setState(() {
      _isLoading = true;
      _isProcessing = true;
      _currentMessage = 'Iniciando $nome...';
      _currentResult = {};
      _currentError = '';
    });

    try {
      await funcao();
    } catch (e) {
      setState(() {
        _currentError = e.toString();
      });
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      body: SafeArea(
        child: Column(
          children: [
            // Header minimalista
            Container(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Text(
                    'Funções Administrativas',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: Colors.grey.shade900,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Gerenciamento do terminal',
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),

            // Card de status
            TransactionStatusCard(
              currentMessage: _currentMessage,
              currentResult: _currentResult,
              currentError: _currentError,
              isProcessing: _isProcessing,
            ),

            const SizedBox(height: 8),

            // Grid de funções
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: GridView.count(
                  crossAxisCount: 2,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                  childAspectRatio: 1.1,
                  children: [
                    _buildFunctionCard(
                      icon: Icons.upload_file_outlined,
                      label: 'Enviar Trace',
                      onTap: () => _executarFuncao(
                        'Envio de Trace',
                            () async {
                          await _sitef.enviarTrace();
                          return 'Trace enviado';
                        },
                      ),
                    ),
                    _buildFunctionCard(
                      icon: Icons.admin_panel_settings_outlined,
                      label: 'Menu Admin',
                      onTap: () => _executarFuncao(
                        'Menu Administrativo',
                            () async {
                          await _sitef.abrirAdmin();
                          return 'Menu aberto';
                        },
                      ),
                    ),
                    _buildFunctionCard(
                      icon: Icons.pending_actions_outlined,
                      label: 'Pendências',
                      onTap: () => _executarFuncao(
                        'Verificação de Pendências',
                            () => _sitef.verificarPendencias(),
                      ),
                    ),
                    _buildFunctionCard(
                      icon: Icons.wifi_tethering_outlined,
                      label: 'Testar Conexão',
                      onTap: () => _executarFuncao(
                        'Teste de Comunicação',
                            () async {
                          await _sitef.testarComunicacao();
                          return 'Conexão OK';
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFunctionCard({
    required IconData icon,
    required String label,
    required Future<void> Function() onTap,
  }) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: Colors.grey.shade300,
          width: 1,
        ),
      ),
      child: InkWell(
        onTap: _isLoading ? null : () async => await onTap(),
        borderRadius: BorderRadius.circular(12),
        child: Container(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 40,
                color: Colors.grey.shade700,
              ),
              const SizedBox(height: 12),
              Text(
                label,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Colors.grey.shade800,
                  letterSpacing: -0.2,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
