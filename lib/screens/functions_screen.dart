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
  final CliSitefEventListener _eventListener = CliSitefEventListener();

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

      if (success) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('Transação concluída'),
            backgroundColor: Colors.grey.shade800,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    });

    _eventListener.errors.listen((error) {
      setState(() {
        _isProcessing = false;
        _currentError = error;
        _currentMessage = '';
      });
    });
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
