package com.example.pinpad_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import br.com.softwareexpress.sitef.android.CliSiTef;
import br.com.softwareexpress.sitef.android.ICliSiTefListener;

public class ClisitefControllerActivity extends AppCompatActivity implements ICliSiTefListener {
    private boolean isActivityRunning = false, isDialogShown = false;
    private CliSiTef clisitef;
    private String textDate, textTime, textTitle;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            //setContentView(R.layout.activity_clisitef);

            DateTime dateTime = new DateTime();
            textDate = dateTime.getCurrentDate();
            textTime = dateTime.getCurrentTime();

            sharedPreferences = getSharedPreferences(TextsSharedPreferences.TEXT_NOME_SHARED.getValor(),
                    MODE_PRIVATE);

            //textViewMessage = findViewById(R.id.textViewMessage);
            //Button buttonCancelar = findViewById(R.id.buttonCancelar);

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

//            buttonCancelar.setOnClickListener(v -> {
//                try {
//                    clisitef.finishTransaction(0);
//                } catch (Exception e) {
//                    Log.d("Erro", "Messege: " + e);
//                    throw new RuntimeException(e);
//                }
//            });

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
                clisitef.continueTransaction("");
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

//            case CliSiTef.CMD_SHOW_QRCODE_FIELD:
//                showQRCodeDialog(clisitef.getBuffer());
//                clisitef.continueTransaction("");
//                break;

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

    private void showMenuDialog(String message) {
        showDialog(message.split(";"), (dialog, which) -> {
            String selectedItem = message.split(";")[which];
            clisitef.continueTransaction(selectedItem);
        });
    }

    private void showConfirmationDialog(String message) {
        showDialog(new String[]{message}, (dialog, which) -> {
            clisitef.continueTransaction("" + which);
            isDialogShown = false;
        });
    }

    private void setFields(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setCancelable(false);

//        View view = getLayoutInflater().inflate(R.layout.input_dialog_view, null);
//        builder.setView(view);

        builder.setPositiveButton("Continuar", (dialog, which) -> {
//            EditText editText = view.findViewById(R.id.edtInputDialog);
            clisitef.continueTransaction("editText.getText().toString()");
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            clisitef.abortTransaction(-1);
            startActivity(new Intent(ClisitefControllerActivity.this,
                    MainActivity.class));
            finish();
        });

        builder.show();
    }

//    private void showQRCodeDialog(String qrcode) {
//        try {
//            DisplayMetrics metrics = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(metrics);
//            int height = metrics.heightPixels;
//            int width = metrics.widthPixels;
//
//            BitMatrix bitMatrix = new MultiFormatWriter().encode(qrcode, BarcodeFormat.QR_CODE,
//                    width, height / 2);
//            Bitmap bitmap = new BarcodeEncoder().createBitmap(bitMatrix);
//
//            View view = getLayoutInflater().inflate(R.layout.show_qrcode_dialog_view, null);
//            ImageView imageView = view.findViewById(R.id.qrCode);
//
//            imageView.setImageBitmap(bitmap);
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setView(view);
//            builder.setNegativeButton("Cancelar", (dialog, which) -> clisitef.abortTransaction(-1));
//            builder.show();
//        } catch (WriterException e) {
//            Log.d("Erro", "Messege: " + e);
//            startActivity(new Intent(ClisitefControllerActivity.this, MainActivity.class));
//            finish();
//        }
//    }

    private void showDialog(String[] items, DialogInterface.OnClickListener positiveAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (items.length == 1) {
            builder.setMessage(items[0]);
            builder.setPositiveButton("Continuar", positiveAction);
        } else {
            builder.setItems(items, positiveAction);
        }

        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            clisitef.abortTransaction(-1);
        });

        builder.show();
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
}