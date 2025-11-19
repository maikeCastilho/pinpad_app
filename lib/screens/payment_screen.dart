import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pinpad_app/service/clisitef_event_listerner.dart';
import '../service/clisitef.dart';
import '../widgets/card.dart';

class PaymentScreen extends StatefulWidget {
  const PaymentScreen({super.key});

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  final SiTefService _sitef = SiTefService();
  final CliSiTefEventListener _eventListener = CliSiTefEventListener();
  final TextEditingController _valorController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _isLoading = false;

  String _currentMessage = '';
  Map<String, dynamic> _currentResult = {};
  String _currentError = '';
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
      final data = event['data'];

      if (type == 'menu_required' && data != null) {
        _showMenuDialog(data as Map<String, dynamic>);
      }

      if (type == 'confirmation_required' && data != null) {
        _showConfirmationDialog(data as Map<String, dynamic>);
      }

      if (type == 'receipt' && data != null && data is Map){
        _showReceipt(Map<String, dynamic>.from(data));
      }

      if (type == 'clear_message') {
        setState(() {
          _currentMessage = '';
        });
      }
    });
  }

  void _showReceipt(Map<String, dynamic> data) {
    final receipt = data['receipt'] as String? ?? '';
    final isClient = data['isClient'] as bool? ?? false;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(isClient ? 'Comprovante - Cliente' : 'Comprovante - Estabelecimento'),
        content: SingleChildScrollView(
          child: Text(
            receipt,
            style: const TextStyle(
              fontFamily: 'Courier',
              fontSize: 12,
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Fechar'),
          ),

        ],
      ),
    );
  }

// ✅ Mostrar dialog de menu
  void _showMenuDialog(Map<String, dynamic> data) {
    final title = data['title'] as String? ?? 'Selecione';
    final options = data['options'] as List<dynamic>? ?? [];

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: SingleChildScrollView( // ✅ Para evitar overflow
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: options.map((option) {
              final optionStr = option.toString();
              // Separar código da descrição: "1:A Vista" -> ["1", "A Vista"]
              final parts = optionStr.split(':');
              final code = parts[0].trim();
              final description = parts.length > 1 ? parts[1].trim() : optionStr;

              return ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(description),
                onTap: () {
                  Navigator.of(context).pop();
                  _sendMenuSelection(code);
                },
              );
            }).toList(),
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

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('Confirmação'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _sitef.sendConfirmation(false); // Não (envia "1")
            },
            child: const Text('Não'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _sitef.sendConfirmation(true); // Sim (envia "0")
            },
            child: const Text('Sim'),
          ),
        ],
      ),
    );
  }

  // ✅ Método para iniciar pagamento
  Future<void> _iniciarPagamento(String modalidade, String nomeModalidade, String restricoes) async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() => _isLoading = true);

    try {
      final valorReais = double.parse(_valorController.text.replaceAll(',', '.'));
      final valorCentavos = (valorReais * 100).toInt().toString();

      await _sitef.iniciarPagamento(
        modalidade: modalidade,
        valor: valorCentavos,
        restricoes: restricoes
      );

      if (mounted) {
        _valorController.clear();
      }

    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('❌ Erro: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  void dispose() {
    _valorController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
    Scaffold(
    backgroundColor: Colors.grey.shade50,
      body: SafeArea(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Text(
                    'Pagamento',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: Colors.grey.shade900,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'insira o valor e escolha a forma de pagamento',
                    style: TextStyle(
                      fontSize: 13,
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),


            // // Card de status
            // TransactionStatusCard(
            //   currentMessage: _currentMessage,
            //   currentResult: _currentResult,
            //   currentError: _currentError,
            //   isProcessing: _isProcessing,
            // ),
            //
            // SizedBox(height: 20),


            // ✅ Campo de Valor
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Form(
                key: _formKey,
                child: TextFormField(
                  controller: _valorController,
                  decoration: InputDecoration(
                    labelText: 'Valor (R\$)',
                    hintText: '10.00',
                    prefixIcon: const Icon(Icons.attach_money),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                    filled: true,
                    fillColor: Colors.grey[50],
                  ),
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'^\d+\.?\d{0,2}')),
                  ],
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Por favor, insira um valor';
                    }
                    final valorDouble = double.tryParse(value.replaceAll(',', '.'));
                    if (valorDouble == null || valorDouble <= 0) {
                      return 'Insira um valor válido';
                    }
                    return null;
                  },
                ),
              ),
            ),

            const SizedBox(height: 30),

            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: GridView.count(
                  crossAxisCount: 2,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                  childAspectRatio: 1.1,
                  children: [
                    // Débito
                    _buildPaymentCard(
                      icon: Icons.credit_card,
                      label: 'Débito',
                      color: Colors.blue,
                      modalidade: '2',
                      restricoes: '',
                    ),

                    // Crédito
                    _buildPaymentCard(
                      icon: Icons.credit_score,
                      label: 'Crédito parcelado',
                      color: Colors.green,
                      modalidade: '3',
                      restricoes: '',
                    ),

                    // PIX
                    _buildPaymentCard(
                      icon: Icons.qr_code,
                      label: 'PIX',
                      color: Colors.teal,
                      modalidade: '122',
                      restricoes: '',
                    ),

                    // Refeicao
                    _buildPaymentCard(
                      icon: Icons.food_bank,
                      label: 'Refeição',
                      color: Colors.blue,
                      modalidade: '5',
                      restricoes: '',
                    ),

                    // // Genérico (Menu)
                    // _buildPaymentCard(
                    //   icon: Icons.menu,
                    //   label: 'Outros',
                    //   color: Colors.orange,
                    //   modalidade: '0',
                    // ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    ),
        if (_isProcessing)
          Container(
            color: Colors.black45,
            child: Center(
              child: Card(
                margin: const EdgeInsets.all(40),
                child: Padding(
                  padding: const EdgeInsets.all(32),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(_getTransactionIcon(), size: 50, color: Colors.black54),
                      const SizedBox(height: 24),
                      Text(
                        _currentMessage,
                        textAlign: TextAlign.center,
                        style: const TextStyle(fontSize: 16, color: Colors.black54, fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }


  IconData _getTransactionIcon() {
      if (_currentMessage.toLowerCase().contains('aproxim') ||
          _currentMessage.toLowerCase().contains('insira') ||
          _currentMessage.toLowerCase().contains('cartao')) {
        return Icons.contactless; // 💳 Aproxime o cartão
      }

      if (_currentMessage.toLowerCase().contains('senha')) {
        return Icons.password; // 🔐 Digite a senha
      }

      if (_currentMessage.toLowerCase().contains('conectando') ||
          _currentMessage.toLowerCase().contains('servidor')) {
        return Icons.cloud_sync; // 🌐 Conectando
      }

      if (_currentMessage.toLowerCase().contains('remov') ||
          _currentMessage.toLowerCase().contains('retire')) {
        return Icons.eject; // 👋 Remova o cartão
      }

      if (_currentMessage.toLowerCase().contains('aprovada')) {
        return Icons.check; // 👋 Remova o cartão
      }

      // Estado de resultado
      if (_currentResult.isNotEmpty) {
        final success = _currentResult['success'] as bool? ?? false;
        return success ? Icons.check_circle : Icons.cancel; // ✅ ou ❌
      }

      return Icons.sync; // ⏳ Processando (padrão)

  }


  // ✅ Widget para criar cards de pagamento
  Widget _buildPaymentCard({
    required IconData icon,
    required String label,
    required Color color,
    required String modalidade,
    required String restricoes
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
        onTap: _isLoading ? null : () => _iniciarPagamento(modalidade, label, restricoes),
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
