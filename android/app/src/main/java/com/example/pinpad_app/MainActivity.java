package com.example.pinpad_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;

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
                        case "iniciarPagamento":
                            String modalidade = call.argument("modalidade");
                            iniciarTransacao(modalidade, result);
                            break;

                        case "verificarPendencias":
                            verificarTransacoesPendentes(result);
                            break;

                        case "abrirAdmin":
                            iniciarTransacao("110", result);
                            break;

                        case "enviarTrace":
                            iniciarTransacao("121", result);
                            break;

                        default:
                            result.notImplemented();
                    }
                });
    }

    private void iniciarTransacao(String modalidade, MethodChannel.Result result) {
        Intent intent = new Intent(MainActivity.this, ClisitefControllerActivity.class);
        intent.putExtra(TextsSharedPreferences.TEXT_MODALIDADE.getValor(), modalidade);
        startActivity(intent);
        result.success("Transação iniciada");
    }

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
                    result.success("Transações pendentes encontradas: " + returnPendingTransactions);
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
}
