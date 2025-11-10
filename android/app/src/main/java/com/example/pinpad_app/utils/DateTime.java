package com.example.pinpad_app.utils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTime {
    // Método para obter a data atual
    public String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    // Método para obter a hora atual
    public String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss", Locale.getDefault());
        Date date = new Date();
        return timeFormat.format(date);
    }
}
