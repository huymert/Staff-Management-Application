package com.example.staff_management.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    /*
     * onAttach:
     * - Dùng để: gắn locale đã lưu vào context.
     * - Kiểm tra/xử lý: lấy ngôn ngữ hiện tại từ SharedPreferences.
     * - Sau đó: app mở lên sẽ dùng đúng ngôn ngữ đã chọn.
     */
    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, Locale.getDefault().getLanguage());
        return setLocale(context, lang);
    }

    /*
     * getLanguage:
     * - Dùng để: lấy mã ngôn ngữ đang dùng.
     * - Kiểm tra/xử lý: đọc từ SharedPreferences.
     * - Sau đó: UI có thể biết đang là tiếng Việt hay tiếng Anh.
     */
    public static String getLanguage(Context context) {
        return getPersistedData(context, Locale.getDefault().getLanguage());
    }

    /*
     * setLocale:
     * - Dùng để: đổi ngôn ngữ cho app.
     * - Kiểm tra/xử lý: lưu lựa chọn rồi cập nhật resources.
     * - Sau đó: giao diện sẽ reload theo ngôn ngữ mới.
     */
    public static Context setLocale(Context context, String language) {
        persist(context, language);
        return updateResources(context, language);
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage);
    }

    private static void persist(Context context, String language) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SELECTED_LANGUAGE, language);
        editor.apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }
}
