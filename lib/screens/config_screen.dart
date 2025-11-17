import 'package:flutter/material.dart';

import '../service/clisitef.dart';

class ConfigScreen extends StatefulWidget {
  const ConfigScreen({super.key});

  @override
  State<ConfigScreen> createState() => _ConfigScreenState();
}

class _ConfigScreenState extends State<ConfigScreen> {
  final SiTefService _sitef = SiTefService();

  // ✅ Controllers para os campos
  final TextEditingController _ipController = TextEditingController(text: "");
  final TextEditingController _lojaController = TextEditingController(text: "");
  // final TextEditingController _terminalController = TextEditingController(text: "SX000001");

  // ✅ Key para o formulário
  final _formKey = GlobalKey<FormState>();

  bool _isLoading = false;

  @override
  void dispose() {
    _ipController.dispose();
    _lojaController.dispose();
    // _terminalController.dispose();
    super.dispose();
  }

  Future<void> _salvarConfiguracao() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isLoading = true;
      });

      try {
        await _sitef.configurarSitef(
          ip: _ipController.text,
          loja: _lojaController.text,
          terminal: "SX000001",
        );

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('✅ Configuração salva com sucesso!'),
              backgroundColor: Colors.green,
            ),
          );
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
          setState(() {
            _isLoading = false;
          });
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.settings, size: 40, color: Colors.grey),
                const SizedBox(width: 10),
                const Text(
                  'Configuração SiTef',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color:  Colors.grey),
                ),
              ],
            ),
            // ✅ Ícone e título

            const SizedBox(height: 40),

            // ✅ Campo: Endereço SiTef
            TextFormField(
              controller: _ipController,
              decoration: const InputDecoration(
                labelText: 'Endereço SiTef',
                hintText: '',
                prefixIcon: Icon(Icons.computer),
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Por favor, insira o endereço IP';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // ✅ Campo: Código da Loja
            TextFormField(
              controller: _lojaController,
              decoration: const InputDecoration(
                labelText: 'Código da Loja',
                hintText: '',
                prefixIcon: Icon(Icons.store),
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
              maxLength: 8,
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Por favor, insira o código da loja';
                }
                if (value.length != 8) {
                  return 'Código deve ter 8 dígitos';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // ✅ Campo: Terminal
            // TextFormField(
            //   controller: _terminalController,
            //   decoration: const InputDecoration(
            //     labelText: 'Terminal',
            //     hintText: 'SX000001',
            //     prefixIcon: Icon(Icons.devices),
            //     border: OutlineInputBorder(),
            //   ),
            //   validator: (value) {
            //     if (value == null || value.isEmpty) {
            //       return 'Por favor, insira o terminal';
            //     }
            //     return null;
            //   },
            // ),
            // const SizedBox(height: 30),

            // ✅ Botão Salvar
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _salvarConfiguracao,
              icon: _isLoading
                  ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              )
                  : const Icon(Icons.save),
              label: Text(_isLoading ? 'Salvando...' : 'Salvar Configuração'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
