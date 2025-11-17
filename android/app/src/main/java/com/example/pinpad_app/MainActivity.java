package com.example.pinpad_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import com.example.pinpad_app.enums.TextsSharedPreferences;
import br.com.softwareexpress.sitef.android.CliSiTef;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "clisitef_channel";
    private CliSiTef clisitef;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pedir permissões de Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        }, 1);
            }
        }

        // Inicializar SharedPreferences e CliSiTef
        sharedPreferences = getSharedPreferences(
                TextsSharedPreferences.TEXT_NOME_SHARED.getValor(),
                MODE_PRIVATE
        );

        clisitef = new CliSiTef(this.getApplicationContext());
        clisitef.setActivity(this);
    }

    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    switch (call.method) {
                        case "configurarSitef":
                            String ip = call.argument("ip");
                            String loja = call.argument("loja");
                            String terminal = call.argument("terminal");
                            configurarSitef(ip, loja, terminal, result);
                            break;

                        case "iniciarPagamento":
                            String valor = call.argument("valor");
                            String modalidade = call.argument("modalidade");
                            iniciarTransacao(modalidade, valor, result);
                            break;

                        case "verificarPendencias":
                            verificarTransacoesPendentes(result);
                            break;

                        case "abrirAdmin":
                            iniciarTransacao("110", "0", result);
                            break;

                        case "enviarTrace":
                            iniciarTransacao("121", "0", result);
                            break;

                        default:
                            result.notImplemented();
                    }
                });
    }

    // ⚙️ Método: Configurar SiTef
    // ⚙️ Método: Configurar SiTef
    private void configurarSitef(String ip, String loja, String terminal, MethodChannel.Result result) {
        try {
            // ✅ Salvar configurações no SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(TextsSharedPreferences.TEXT_ENDERECO_SITEF.getValor(), ip);
            editor.putString(TextsSharedPreferences.TEXT_CODIGO_LOJA.getValor(), loja);
            editor.putString(TextsSharedPreferences.TEXT_NUMERO_TERMINAL.getValor(), terminal);

            // Valores padrão obrigatórios
            String timestamp = String.valueOf(System.currentTimeMillis());
            editor.putString(TextsSharedPreferences.TEXT_CUPOM_FISCAL.getValor(), timestamp);
            editor.putString(TextsSharedPreferences.TEXT_OPERADOR.getValor(), "0001");
            editor.putString(TextsSharedPreferences.TEXT_RESTRICOES.getValor(), "");
            editor.apply();

            // ✅ CHAMAR CONFIGURE AQUI - UMA ÚNICA VEZ
            String paramAdicionais = "[TipoComunicacaoExterna="
                    + sharedPreferences.getString(
                    TextsSharedPreferences.TEXT_COM_EXTERNA.getValor(), "")
                    + ";ParmsClient=1=" + sharedPreferences.getString(
                    TextsSharedPreferences.TEXT_CNPJ_AUTOMACAO.getValor(), "")
                    + ";2=" + sharedPreferences.getString(
                    TextsSharedPreferences.TEXT_CNPJ_FACILITADOR.getValor(), "") + "]";

            int returnConfig = clisitef.configure(ip, loja, terminal, paramAdicionais);

            Log.e("DATA-CONFIG", "IP:" + ip + "LOJA:" + loja + "TERM:" + terminal + "PARAMS:" + paramAdicionais);

            if (returnConfig == 0) {
                result.success("Configuração salva e CliSiTef configurado com sucesso");
            } else {
                result.error("ERRO_CONFIG", "Erro ao configurar CliSiTef: " + returnConfig, null);
            }

        } catch (Exception e) {
            result.error("ERRO", "Erro ao salvar configuração: " + e.getMessage(), null);
        }
    }



    // 💳 Método: Iniciar Transação
    private void iniciarTransacao(String modalidade, String valor, MethodChannel.Result result) {
        try {
            // Salvar valor no SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(TextsSharedPreferences.TEXT_VALOR.getValor(), valor);
            editor.putString(TextsSharedPreferences.TEXT_MODALIDADE.getValor(), modalidade);
            editor.apply();

            // Iniciar ClisitefControllerActivity
            Intent intent = new Intent(MainActivity.this, ClisitefControllerActivity.class);
            intent.putExtra(TextsSharedPreferences.TEXT_MODALIDADE.getValor(), modalidade);
            startActivity(intent);

            result.success("Transação iniciada");

        } catch (Exception e) {
            result.error("ERRO", "Erro ao iniciar transação: " + e.getMessage(), null);
        }
    }

    // 🔍 Método: Verificar Pendências
    private void verificarTransacoesPendentes(MethodChannel.Result result) {
        if (sharedPreferences.contains(TextsSharedPreferences.TEXT_DATA_FISCAL.getValor())) {
            try {
                int returnPendingTransactions = clisitef.getQttPendingTransactions(
                        sharedPreferences.getString(TextsSharedPreferences.TEXT_DATA_FISCAL.getValor(), ""),
                        sharedPreferences.getString(TextsSharedPreferences.TEXT_CUPOM_FISCAL.getValor(), "")
                );

                if (returnPendingTransactions > 0) {
                    Intent intent = new Intent(MainActivity.this, ClisitefControllerActivity.class);
                    intent.putExtra("abortarTransacao", true);
                    startActivity(intent);
                    result.success("Transações pendentes: " + returnPendingTransactions);
                } else {
                    result.success("Nenhuma transação pendente");
                }
            } catch (Exception e) {
                result.error("ERRO", "Erro ao verificar pendências: " + e.getMessage(), null);
            }
        } else {
            result.success("Nenhuma transação registrada");
        }
    }

    // ❌ Método: Cancelar Transação
    private void cancelarTransacao(String nsu, String data, MethodChannel.Result result) {
        try {
            Intent intent = new Intent(MainActivity.this, ClisitefControllerActivity.class);
            intent.putExtra(TextsSharedPreferences.TEXT_MODALIDADE.getValor(), "200"); // Cancelamento
            intent.putExtra("nsu", nsu);
            intent.putExtra("data", data);
            startActivity(intent);

            result.success("Cancelamento iniciado");
        } catch (Exception e) {
            result.error("ERRO", "Erro ao cancelar: " + e.getMessage(), null);
        }
    }


}
