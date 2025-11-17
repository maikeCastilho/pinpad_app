import 'package:flutter/material.dart';
import 'package:pinpad_app/screens/config_screen.dart';
import 'package:pinpad_app/screens/functions_screen.dart';
import 'package:pinpad_app/screens/payment_screen.dart';
import 'package:pinpad_app/service/clisitef.dart';

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

  // ✅ Controle da navegação
  int _selectedIndex = 0;

  // ✅ Lista de páginas/telas
  final List<Widget> _pages = [
    const PaymentScreen(),
    const FunctionsScreen(),
    const ConfigScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('PinPad - CliSiTef', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),),
        backgroundColor: Colors.blue
      ),

      // ✅ Exibir a página selecionada
      body: _pages[_selectedIndex],

      // ✅ Bottom Navigation Bar
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: (index) {
          setState(() {
            _selectedIndex = index;
          });
        },
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.payment),
            label: 'Pagamento',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.construction),
            label: 'Admin',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Config',
          ),
        ],
      ),
    );
  }
}
