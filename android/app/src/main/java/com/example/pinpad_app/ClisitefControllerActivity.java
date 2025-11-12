package com.example.pinpad_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pinpad_app.enums.TextsSharedPreferences;
import com.example.pinpad_app.enums.Transaction;
import com.example.pinpad_app.utils.DateTime;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import android.os.Handler;
import android.os.Looper;
import br.com.softwareexpress.sitef.android.CliSiTef;
import br.com.softwareexpress.sitef.android.ICliSiTefListener;

public class ClisitefControllerActivity extends AppCompatActivity implements ICliSiTefListener {
    private boolean isActivityRunning = false, isDialogShown = false;
    private CliSiTef clisitef;
    private String textDate, textTime, textTitle;
    private SharedPreferences sharedPreferences;
    private AlertDialog currentDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            DateTime dateTime = new DateTime();
            textDate = dateTime.getCurrentDate();
            textTime = dateTime.getCurrentTime();

            sharedPreferences = getSharedPreferences(TextsSharedPreferences.TEXT_NOME_SHARED.getValor(),
                    MODE_PRIVATE);

            // TODO: Instanciando a classe CliSITef.
            clisitef = new CliSiTef(this.getApplicationContext());
            clisitef.setActivity(this);

            // TODO: Concatenar aqui todos os dados do paramAdicionais.
            String paramAdicionais = "[TipoComunicacaoExterna="
                    + sharedPreferences.getString(
                    TextsSharedPreferences.TEXT_COM_EXTERNA.getValor(), "")
                    + ";ParmsClient=1=" + sharedPreferences.getString(
                    TextsSharedPreferences.TEXT_CNPJ_AUTOMACAO.getValor(), "")
                    + ";2=" + sharedPreferences.getString(
                    TextsSharedPreferences.TEXT_CNPJ_FACILITADOR.getValor(), "") + "]";

            if(sharedPreferences.contains(TextsSharedPreferences.TEXT_PARAMETROS_ADICIONAIS.getValor()))
                // Caso passar o param adicinais pelo app, será desconsiderado o default.
                paramAdicionais = sharedPreferences.getString(
                        TextsSharedPreferences.TEXT_PARAMETROS_ADICIONAIS.getValor(), "");

            // TODO: Abortar Transação caso tenha transação pendentes.
            if (getIntent().getBooleanExtra("abortarTransacao", false)) {
                clisitef.finishTransaction(this,
                        // Caso queira aceitar a trasnação, basta mudar o ENUM.
                        Transaction.CANCELAR_TRANSACAO.getValor(),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_CUPOM_FISCAL.getValor(), ""),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_DATA_FISCAL.getValor(), ""),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_HORARIO.getValor(), ""),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_PARAMETROS_ADICIONAIS.getValor(),
                                paramAdicionais));
                Toast.makeText(this, "Transações Pendentes Canceladas.", Toast.LENGTH_SHORT).show();
            }

            // Pegando o campo da modalidade e convertendo em int.
            int textModalidade = Integer.parseInt(getIntent().getStringExtra("textModalidade"));

            // TODO: Pegando o retorno do médoo Configure.
            int returnConfig = clisitef.configure(
                    sharedPreferences.getString(
                            TextsSharedPreferences.TEXT_ENDERECO_SITEF.getValor(), ""),
                    sharedPreferences.getString(
                            TextsSharedPreferences.TEXT_CODIGO_LOJA.getValor(), ""),
                    sharedPreferences.getString(
                            TextsSharedPreferences.TEXT_NUMERO_TERMINAL.getValor(), ""),
                    paramAdicionais);

            // TODO: Caso o retorno seja igual a 0, ele continuará.
            if (returnConfig == Transaction.RETORNO_CONFIGURE.getValor()) {
                int returnStartTransaction = clisitef.startTransaction(
                        this,
                        textModalidade,
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_VALOR.getValor(), "0"),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_CUPOM_FISCAL.getValor(), ""),
                        textDate,
                        dateTime.getCurrentTime(),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_OPERADOR.getValor(), ""),
                        sharedPreferences.getString(
                                TextsSharedPreferences.TEXT_RESTRICOES.getValor(), ""));
                if (returnStartTransaction != Transaction.RETORNO_START_TRANSACTION.getValor()) {
                    // TODO: Caso seja diferente de 0, ele acabará
                    Toast.makeText(this, "Erro na startTransaction",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ClisitefControllerActivity.this,
                            MainActivity.class));
                    finish();
                }
            } else {
                // TODO: Caso seja diferente de 0, ele acabará
                Toast.makeText(this, "Erro na configuração", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ClisitefControllerActivity.this,
                        MainActivity.class));
                finish();
            }

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    // Desativando o botão de voltar.
                }
            });
        } catch (Exception e) {
            Log.d("Erro", "Messege: " + e);
            Toast.makeText(this, "Ocorreu um erro inesperado, verifique os logs.",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ClisitefControllerActivity.this,
                    MainActivity.class));
            finish();
        }
    }


    @Override
    public void onData(int currentStage, int command, int fieldId, int minLength, int maxLength, byte[] input) {
        switch (command) {
            case CliSiTef.CMD_RESULT_DATA:
                if (fieldId == Transaction.CAMPO_COMPROVANTE_CLIENTE.getValor()
                        || fieldId == Transaction.CAMPO_COMPROVANTE_ESTAB.getValor()) {
                    showConfirmationDialog(clisitef.getBuffer());
                }
                else
                    clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_SHOW_MSG_CASHIER:
            case CliSiTef.CMD_SHOW_MSG_CUSTOMER:
            case CliSiTef.CMD_SHOW_MSG_CASHIER_CUSTOMER:
                clisitef.continueTransaction("Insira ou Aproxime o cartão!");
                break;

            case CliSiTef.CMD_SHOW_MENU_TITLE:
            case CliSiTef.CMD_SHOW_HEADER:
                textTitle = clisitef.getBuffer();
                clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_CLEAR_MSG_CASHIER:
            case CliSiTef.CMD_CLEAR_MSG_CUSTOMER:
            case CliSiTef.CMD_CLEAR_MSG_CASHIER_CUSTOMER:
            case CliSiTef.CMD_CLEAR_MENU_TITLE:
            case CliSiTef.CMD_CLEAR_HEADER:
                clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_CONFIRM_GO_BACK:
            case CliSiTef.CMD_CONFIRMATION:
                showConfirmationDialog(clisitef.getBuffer());
                break;

            case CliSiTef.CMD_GET_FIELD_CURRENCY:
            case CliSiTef.CMD_GET_FIELD_BARCODE:
            case CliSiTef.CMD_GET_FIELD:
                setFields(textTitle);
                break;

            case CliSiTef.CMD_GET_MENU_OPTION:
                showMenuDialog(clisitef.getBuffer());
                break;

            case CliSiTef.CMD_PRESS_ANY_KEY:
                showConfirmationDialog(clisitef.getBuffer());
                break;

            case CliSiTef.CMD_SHOW_QRCODE_FIELD:
                showQRCodeDialog(clisitef.getBuffer());
                clisitef.continueTransaction("");
                break;

            case CliSiTef.CMD_REMOVE_QRCODE_FIELD:
                clisitef.continueTransaction("");
                break;

            default:
                clisitef.continueTransaction("");
                break;
        }
    }


    @Override
    public void onTransactionResult(int currentStage, int resultCode) {
        if (currentStage == 1 && resultCode == 0) {
            try {
                clisitef.finishTransaction(1);

                // TODO: Salvando a data da transação feita para verificar se há transação pendete.
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString(TextsSharedPreferences.TEXT_DATA_FISCAL.getValor(), textDate);
                edit.putString(TextsSharedPreferences.TEXT_HORARIO.getValor(), textTime);
                edit.apply();

                startActivity(new Intent(ClisitefControllerActivity.this,
                        MainActivity.class));
                finish();
            } catch (Exception e) {
                Log.d("Erro", "Messege: " + e);
                finish();
            }
        } else if (currentStage == 2 && resultCode == 0) {
            if (!isDialogShown) {
                isDialogShown = true;
                runOnUiThread(() -> {
                    if (isActivityRunning) {
                        showConfirmationDialog(clisitef.getBuffer());
                    } else {
                        isDialogShown = false;
                    }
                });
            }
        } else {
            if (resultCode != 0) {
                startActivity(new Intent(ClisitefControllerActivity.this,
                        MainActivity.class));
                finish();
            }
        }
    }


    // ✅ MÉTODOS DE DIALOG ADAPTADOS (sem XML)

    private void showMessageDialog(String message) {
        runOnUiThread(() -> {
            dismissCurrentDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message);
            builder.setCancelable(false);
            currentDialog = builder.show();
        });
    }

    private void showMenuDialog(String message) {
        runOnUiThread(() -> {
            dismissCurrentDialog();
            String[] items = message.split(";");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Selecione uma opção");
            builder.setCancelable(false);
            builder.setItems(items, (dialog, which) -> {
                String selectedItem = items[which];
                clisitef.continueTransaction(selectedItem);
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> {
                clisitef.abortTransaction(-1);
                finish();
            });
            currentDialog = builder.show();
        });
    }

    private void showConfirmationDialog(String message) {
        runOnUiThread(() -> {
            dismissCurrentDialog();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message);
            builder.setCancelable(false);
            builder.setPositiveButton("Continuar", (dialog, which) -> {
                clisitef.continueTransaction("0");
                isDialogShown = false;
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> {
                clisitef.abortTransaction(-1);
                isDialogShown = false;
                finish();
            });
            currentDialog = builder.show();
        });
    }

    private void setFields(String title) {
        runOnUiThread(() -> {
            dismissCurrentDialog();

            // Criar EditText programaticamente
            EditText editText = new EditText(this);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setHint("Digite aqui");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);
            layout.addView(editText);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);
            builder.setView(layout);
            builder.setCancelable(false);
            builder.setPositiveButton("Continuar", (dialog, which) -> {
                clisitef.continueTransaction(editText.getText().toString());
            });
            builder.setNegativeButton("Cancelar", (dialog, which) -> {
                clisitef.abortTransaction(-1);
                finish();
            });
            currentDialog = builder.show();
        });
    }

    private void showQRCodeDialog(String qrcode) {
        runOnUiThread(() -> {
            try {
                dismissCurrentDialog();

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int height = metrics.heightPixels;
                int width = metrics.widthPixels;

                BitMatrix bitMatrix = new MultiFormatWriter().encode(
                        qrcode,
                        BarcodeFormat.QR_CODE,
                        width,
                        height / 2
                );
                Bitmap bitmap = new BarcodeEncoder().createBitmap(bitMatrix);

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setPadding(20, 20, 20, 20);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("QR Code");
                builder.setView(imageView);
                builder.setCancelable(false);
                builder.setNegativeButton("Cancelar", (dialog, which) -> {
                    clisitef.abortTransaction(-1);
                    finish();
                });
                currentDialog = builder.show();
            } catch (WriterException e) {
                Log.e("ClisitefController", "Erro ao gerar QRCode: " + e.getMessage());
                Toast.makeText(this, "Erro ao gerar QR Code", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("ClisitefController", "🔴 onDestroy() - Limpando recursos");

        // Fechar dialogs abertos
        dismissCurrentDialog();

        // Limpar CliSiTef
        if (clisitef != null) {
            try {
                // Tentar abortar qualquer transação pendente
                clisitef.abortTransaction(-1);
                clisitef.setActivity(null);
                clisitef = null;
            } catch (Exception e) {
                Log.e("ClisitefController", "Erro ao limpar CliSiTef: " + e.getMessage());
            }
        }

        isActivityRunning = false;
    }

}
