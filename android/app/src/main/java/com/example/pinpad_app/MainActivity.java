package com.example.pinpad_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

import com.example.pinpad_app.enums.TextsSharedPreferences;
import com.example.pinpad_app.enums.Transaction;
import com.example.pinpad_app.utils.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.softwareexpress.sitef.android.CliSiTef;
import br.com.softwareexpress.sitef.android.ICliSiTefListener;

public class MainActivity extends FlutterActivity implements ICliSiTefListener {
    private static final String METHOD_CHANNEL = "clisitef_channel";
    private static final String EVENT_CHANNEL = "clisitef_events";

    private static EventChannel.EventSink eventSink;
    private CliSiTef clisitef;
    private SharedPreferences sharedPreferences;
    private boolean isTransactionRunning = false;
    private String textTitle;

    // Variáveis para comunicação Flutter → Native
    private static String pendingMenuSelection = null;
    private static Boolean pendingConfirmation = null;
    private static final Object menuLock = new Object();
    private static final Object confirmLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(
                TextsSharedPreferences.TEXT_NOME_SHARED.getValor(),
                MODE_PRIVATE
        );

        clisitef = new CliSiTef(getApplicationContext());
        clisitef.setActivity(this);
    }

    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        // MethodChannel
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), METHOD_CHANNEL)
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
                            String restricoes = call.argument("restricoes");
                            iniciarTransacao(modalidade, valor, restricoes != null ? restricoes : "", result);
                            break;

                        case "enviarTrace":
                            iniciarTransacao("121", "0", "", result);
                            break;

                        case "abrirAdmin":
                            iniciarTransacao("110", "0", "", result);
                            break;

                        case "testarConexao":
                            iniciarTransacao("111", "0", "", result);
                            break;

                        case "sendMenuSelection":
                            String selectedOption = call.argument("option");
                            sendMenuSelection(selectedOption);
                            result.success(null);
                            break;

                        case "sendConfirmation":
                            Boolean confirmed = call.argument("confirmed");
                            sendConfirmation(confirmed);
                            result.success(null);
                            break;

                        default:
                            result.notImplemented();
                    }
                });

        // EventChannel
        new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), EVENT_CHANNEL)
                .setStreamHandler(new EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, EventChannel.EventSink events) {
                        eventSink = events;
                        Log.d("EventChannel", "Flutter começou a escutar eventos");
                    }

                    @Override
                    public void onCancel(Object arguments) {
                        eventSink = null;
                        Log.d("EventChannel", "Flutter parou de escutar eventos");
                    }
                });
    }

    private void configurarSitef(String ip, String loja, String terminal, MethodChannel.Result result) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(TextsSharedPreferences.TEXT_ENDERECO_SITEF.getValor(), ip);
            editor.putString(TextsSharedPreferences.TEXT_CODIGO_LOJA.getValor(), loja);
            editor.putString(TextsSharedPreferences.TEXT_NUMERO_TERMINAL.getValor(), terminal);
            editor.apply();

            result.success("Configuração salva com sucesso");
        } catch (Exception e) {
            result.error("ERRO", "Erro ao configurar: " + e.getMessage(), null);
        }
    }

    private void iniciarTransacao(String modalidade, String valor, String restricoes, MethodChannel.Result result) {
        if (isTransactionRunning) {
            result.error("TRANSACTION_RUNNING", "Já existe uma transação em andamento", null);
            return;
        }

        try {
            isTransactionRunning = true;
            sendEvent("transaction_started", "Transação iniciada");

            DateTime dateTime = new DateTime();
            String data = dateTime.getCurrentDate();
            String hora = dateTime.getCurrentTime();

            String paramAdicionais = "[TipoComunicacaoExterna="
                    + sharedPreferences.getString(TextsSharedPreferences.TEXT_COM_EXTERNA.getValor(), "")
                    + ";ParmsClient=1=" + sharedPreferences.getString(TextsSharedPreferences.TEXT_CNPJ_AUTOMACAO.getValor(), "")
                    + ";2=" + sharedPreferences.getString(TextsSharedPreferences.TEXT_CNPJ_FACILITADOR.getValor(), "") + "]";

            int returnConfig = clisitef.configure(
                    sharedPreferences.getString(TextsSharedPreferences.TEXT_ENDERECO_SITEF.getValor(), ""),
                    sharedPreferences.getString(TextsSharedPreferences.TEXT_CODIGO_LOJA.getValor(), ""),
                    sharedPreferences.getString(TextsSharedPreferences.TEXT_NUMERO_TERMINAL.getValor(), ""),
                    paramAdicionais);

            if (returnConfig == Transaction.RETORNO_CONFIGURE.getValor()) {
                int returnStartTransaction = clisitef.startTransaction(
                        this,
                        Integer.parseInt(modalidade),
                        valor,
                        "",
                        data,
                        hora,
                        "0001",
                        restricoes);

                if (returnStartTransaction == Transaction.RETORNO_START_TRANSACTION.getValor()) {
                    result.success("Transação iniciada");
                } else {
                    isTransactionRunning = false;
                    result.error("START_ERROR", "Erro ao iniciar transação", null);
                }
            } else {
                isTransactionRunning = false;
                result.error("CONFIG_ERROR", "Erro na configuração", null);
            }
        } catch (Exception e) {
            isTransactionRunning = false;
            result.error("ERRO", "Erro ao iniciar transação: " + e.getMessage(), null);
        }
    }

    public static void sendEvent(String type, Object data) {
        if (eventSink != null) {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("data", data);
            event.put("timestamp", System.currentTimeMillis());
            eventSink.success(event);
            Log.d("EventChannel", "Evento enviado: " + type);
        }
    }

    public static void sendMenuSelection(String selection) {
        synchronized (menuLock) {
            pendingMenuSelection = selection;
            menuLock.notifyAll();
        }
    }

    public static void sendConfirmation(Boolean confirmed) {
        synchronized (confirmLock) {
            pendingConfirmation = confirmed;
            confirmLock.notifyAll();
        }
    }

    @Override
    public void onData(int currentStage, int command, int fieldId, int minLength, int maxLength, byte[] input) {
        Log.d("Command", "Command: " + command + ", FieldId: " + fieldId);

        switch (command) {
            case CliSiTef.CMD_RESULT_DATA:
                String bufferData = clisitef.getBuffer();

                // ✅ LOG DE TODOS OS CAMPOS
                Log.d("CAMPO", "fieldId=" + fieldId + ", value=" + bufferData);

                // ✅ ENVIAR TODOS PARA FLUTTER
                Map<String, Object> fieldData = new HashMap<>();
                fieldData.put("fieldId", fieldId);
                fieldData.put("value", bufferData);
                sendEvent("field_data", fieldData);

                // Tratamento especial para comprovantes
                if (fieldId == Transaction.CAMPO_COMPROVANTE_CLIENTE.getValor()
                        || fieldId == Transaction.CAMPO_COMPROVANTE_ESTAB.getValor()) {
                    Map<String, Object> receiptData = new HashMap<>();
                    receiptData.put("receipt", bufferData);
                    receiptData.put("fieldId", fieldId);
                    receiptData.put("isClient", fieldId == Transaction.CAMPO_COMPROVANTE_CLIENTE.getValor());
                    sendEvent("receipt", receiptData);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (clisitef != null) {
                            clisitef.continueTransaction("0");
                        }
                    }, 2000);
                } else {
                    clisitef.continueTransaction("");
                }
                break;

            case CliSiTef.CMD_SHOW_MSG_CASHIER:

            case CliSiTef.CMD_SHOW_MSG_CUSTOMER:

            case CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER:
                String mensagem = clisitef.getBuffer();
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("message", mensagem);
                messageData.put("command", command);
                sendEvent("message", messageData);
                clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_SHOW_MENU_TITLE:

            case CliSiTef.CMD_SHOW_HEADER:
                textTitle = clisitef.getBuffer();
                Map<String, Object> titleData = new HashMap<>();
                titleData.put("title", textTitle);
                sendEvent("title", titleData);
                clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_CLEAR_MSG_CASHIER:

            case CliSiTef.CMD_CLEAR_MSG_CUSTOMER:

            case CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER:

            case CliSiTef.CMD_CLEAR_MENU_TITLE:

            case CliSiTef.CMD_CLEAR_HEADER:
                sendEvent("clear_message", null);
                clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_CONFIRM_GO_BACK:

            case CliSiTef.CMD_CONFIRMATION:
                String confirmMessage = clisitef.getBuffer();
                Map<String, Object> confirmData = new HashMap<>();
                confirmData.put("message", confirmMessage);
                confirmData.put("requiresUserAction", true);
                sendEvent("confirmation_required", confirmData);

                new Thread(() -> {
                    try {
                        synchronized (confirmLock) {
                            pendingConfirmation = null;
                            confirmLock.wait(30000);

                            Boolean confirmed = pendingConfirmation;
                            if (confirmed != null) {
                                clisitef.continueTransaction(confirmed ? "0" : "1");
                            } else {
                                clisitef.continueTransaction("0");
                            }
                        }
                    } catch (InterruptedException e) {
                        clisitef.continueTransaction("0");
                    }
                }).start();
                break;


            case CliSiTef.CMD_GET_MENU_OPTION:
                String menuOptions = clisitef.getBuffer();
                String[] options = menuOptions.split(";");

                Map<String, Object> menuData = new HashMap<>();
                menuData.put("title", textTitle != null ? textTitle : "Selecione");
                List<String> optionsList = new ArrayList<>(Arrays.asList(options));
                menuData.put("options", optionsList);
                menuData.put("requiresUserAction", true);
                sendEvent("menu_required", menuData);

                new Thread(() -> {
                    try {
                        synchronized (menuLock) {
                            pendingMenuSelection = null;
                            menuLock.wait(30000);

                            String selectedOption = pendingMenuSelection;
                            if (selectedOption != null) {
                                clisitef.continueTransaction(selectedOption);
                            } else if (options.length > 0) {
                                String firstOption = options[0].split(":")[0].trim();
                                clisitef.continueTransaction(firstOption);
                            }
                        }
                    } catch (InterruptedException e) {
                        if (options.length > 0) {
                            String firstOption = options[0].split(":")[0].trim();
                            clisitef.continueTransaction(firstOption);
                        }
                    }
                }).start();
                break;



            default:
                clisitef.continueTransaction("");
                break;
        }
    }

    @Override
    public void onTransactionResult(int currentStage, int resultCode) {
        Log.d("MainActivity", "=== onTransactionResult ===");
        Log.d("MainActivity", "CurrentStage: " + currentStage + ", ResultCode: " + resultCode);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("stage", currentStage);
        resultData.put("resultCode", resultCode);
        resultData.put("success", resultCode == 0);
        sendEvent("transaction_result", resultData);

        if (currentStage == 1 && resultCode == 0) {
            try {
                clisitef.finishTransaction(1);
            } catch (Exception e) {
                Log.e("MainActivity", "Erro no finishTransaction: " + e.getMessage());
                sendEvent("error", e.getMessage());
                isTransactionRunning = false;
            }
        } else if (currentStage == 2 && resultCode == 0) {
            Log.d("MainActivity", "✅ Transação concluída com sucesso");
            isTransactionRunning = false;
            sendEvent("transaction_finished", null);
        } else {
            if (resultCode != 0) {
                sendEvent("error", "Transação falhou: código " + resultCode);
                isTransactionRunning = false;
            }
        }
    }
}
