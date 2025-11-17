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
  final CliSitefEventListener _eventListener = CliSitefEventListener();
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
    // ✅ Escutar mensagens
    _eventListener.messages.listen((message) {
      setState(() {
        _currentMessage = message;
      });
      print('📝 Mensagem: $message');
    });

    // // ✅ Escutar resultados
    // _eventListener.results.listen((result) {
    //   final data = result['data'] as Map<String, dynamic>;
    //   final success = data['success'] as bool;
    //
    //   setState(() {
    //     _isProcessing = false;
    //   });
    //
    //   if (success) {
    //     ScaffoldMessenger.of(context).showSnackBar(
    //       const SnackBar(
    //         content: Text('✅ Transação aprovada!'),
    //         backgroundColor: Colors.green,
    //       ),
    //     );
    //   }
    }

  // ✅ Método para iniciar pagamento
  Future<void> _iniciarPagamento(String modalidade, String nomeModalidade) async {
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
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('✅ $nomeModalidade iniciado!'),
            backgroundColor: Colors.green,
          ),
        );
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
    return Padding(
      padding: const EdgeInsets.all(16.0),
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


          // Card de status
          TransactionStatusCard(
            currentMessage: _currentMessage,
            currentResult: _currentResult,
            currentError: _currentError,
            isProcessing: _isProcessing,
          ),

          SizedBox(height: 20),


          // ✅ Campo de Valor
          Form(
            key: _formKey,
            child: TextFormField(
              controller: _valorController,
              decoration: InputDecoration(
                labelText: 'Valor (R\$)',
                hintText: '10.00',
                prefixIcon: const Icon(Icons.attach_money),
                border: const OutlineInputBorder(),
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
          const SizedBox(height: 30),

          // ✅ Grid de Opções de Pagamento (2x2)
          Expanded(
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
                ),

                // Crédito
                _buildPaymentCard(
                  icon: Icons.credit_score,
                  label: 'Crédito',
                  color: Colors.green,
                  modalidade: '3',
                ),

                // PIX
                _buildPaymentCard(
                  icon: Icons.qr_code,
                  label: 'PIX',
                  color: Colors.teal,
                  modalidade: '122', // Código PIX (verificar na documentação)
                ),

                // Refeicao
                _buildPaymentCard(
                  icon: Icons.food_bank,
                  label: 'Refeição',
                  color: Colors.blue,
                  modalidade: '5',
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

          // ✅ Indicador de Loading
          if (_isLoading)
            Container(
              margin: const EdgeInsets.only(top: 20),
              child: const Column(
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 10),
                  Text('Processando pagamento...', style: TextStyle(color: Colors.grey)),
                ],
              ),
            ),
        ],
      ),
    );
  }

  // ✅ Widget para criar cards de pagamento
  Widget _buildPaymentCard({
    required IconData icon,
    required String label,
    required Color color,
    required String modalidade,
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
        onTap: _isLoading ? null : () => _iniciarPagamento(modalidade, label),
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
