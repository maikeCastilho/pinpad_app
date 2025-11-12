import 'package:flutter/material.dart';
import 'package:pinpad_app/clisitef.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PinPad App',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final SiTefService _sitef = SiTefService();
  final TextEditingController _valorController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('PinPad - CliSiTef'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Campo de valor
            TextField(
              controller: _valorController,
              decoration: const InputDecoration(
                labelText: 'Valor (em centavos)',
                hintText: '1000 = R\$ 10,00',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 20),

            ElevatedButton.icon(
              onPressed: () async {
                print("SUPER");
                await _sitef.configurarSitef(ip: "172.30.0.92", loja: "00000000", terminal: "");
              },
              icon: const Icon(Icons.payment),
              label: const Text('Configurar Sitef!!!!'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
              ),
            ),
            const SizedBox(height: 10),

            ElevatedButton.icon(
              onPressed: () async {
                print("SUPER");
                await _sitef.enviarTrace();
              },
              icon: const Icon(Icons.payment),
              label: const Text('Enviar Trace'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
              ),
            ),
            const SizedBox(height: 10),

            // ElevatedButton.icon(
            //   onPressed: () async {
            //     // 1️⃣ PRIMEIRO: Configurar
            //     print("🔧 Configurando...");
            //     await _sitef.configurarSitef(
            //       ip: "172.30.0.92",
            //       loja: "00000000",
            //       terminal: "SX000001",
            //     );
            //
            //     // 2️⃣ SEGUNDO: Enviar Trace
            //     print("📡 Enviando Trace...");
            //     await _sitef.enviarTrace();
            //   },
            //   style: ElevatedButton.styleFrom(
            //     minimumSize: const Size(double.infinity, 50),
            //   ),
            //   icon: const Icon(Icons.sync),
            //   label: const Text('Configurar + Enviar Trace'),
            // ),
            //
            // const SizedBox(height: 10),

            // Botão Pagamento
            ElevatedButton.icon(
              onPressed: () async {
                // ✅ VALIDAR VALOR
                String valor = _valorController.text.trim();

                if (valor.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('❌ Digite um valor!')),
                  );
                  return;
                }

                // ✅ GARANTIR QUE É APENAS NÚMEROS
                if (!RegExp(r'^\d+$').hasMatch(valor)) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('❌ Valor deve conter apenas números!')),
                  );
                  return;
                }

                // ✅ LIMITAR TAMANHO (máximo 10 dígitos = R$ 99.999.999,99)
                if (valor.length > 10) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('❌ Valor muito grande!')),
                  );
                  return;
                }

                print("🔧 Configurando...");
                await _sitef.configurarSitef(
                  ip: "172.30.0.92",
                  loja: "00000000",
                  terminal: "SX000001",
                );

                print("💳 Iniciando pagamento: R\$ ${(int.parse(valor) / 100).toStringAsFixed(2)}");

                await _sitef.iniciarPagamento(valor: valor);
              },
              icon: const Icon(Icons.payment),
              label: const Text('Iniciar Pagamento'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
              ),
            ),

            const SizedBox(height: 10),

            // Botão Admin
            ElevatedButton.icon(
              onPressed: () async {
                await _sitef.abrirAdmin();
              },
              icon: const Icon(Icons.settings),
              label: const Text('Menu Administrativo'),
              style: ElevatedButton.styleFrom(
                minimumSize: const Size(double.infinity, 50),
              ),
            ),
            const SizedBox(height: 10),

            // Botão Verificar Pendências
            // ElevatedButton.icon(
            //   onPressed: () async {
            //     final result = await _sitef.verificarPendencias();
            //     if (mounted) {
            //       ScaffoldMessenger.of(context).showSnackBar(
            //         SnackBar(content: Text(result ?? 'Erro')),
            //       );
            //     }
            //   },
            //   icon: const Icon(Icons.check_circle),
            //   label: const Text('Verificar Pendências'),
            //   style: ElevatedButton.styleFrom(
            //     minimumSize: const Size(double.infinity, 50),
            //   ),
            // ),
          ],
        ),
      ),
    );
  }
}
