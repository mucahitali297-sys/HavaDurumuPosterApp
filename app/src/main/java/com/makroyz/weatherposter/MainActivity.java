package com.makroyz.weatherposter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final Locale tr = new Locale("tr", "TR");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    private LinearLayout content;
    private LinearLayout forecastContainer;
    private TextView cityText;
    private TextView dateText;
    private TextView tempText;
    private TextView weatherText;
    private TextView feelsText;
    private TextView humidityText;
    private TextView windText;
    private TextView rainText;
    private TextView statusText;
    private TextView updateText;

    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            loadWeather(prefs.getString("city", "İstanbul"));
            handler.postDelayed(this, 10 * 60 * 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setNavigationBarColor(Color.rgb(5, 8, 22));
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        prefs = getSharedPreferences("weather_poster", MODE_PRIVATE);
        buildUi();
        String city = prefs.getString("city", "İstanbul");
        loadWeather(city);
        handler.postDelayed(autoRefresh, 10 * 60 * 1000);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(autoRefresh);
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(5, 8, 22), Color.rgb(14, 27, 58), Color.rgb(6, 13, 32)});
        root.setBackground(bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(18), dp(26), dp(18), dp(20));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView brand = text("CANLI HAVA DURUMU", 15, 0xFF7DD3FC, true);
        brand.setLetterSpacing(0.12f);
        brand.setGravity(Gravity.CENTER);
        content.addView(brand, matchWrap());

        cityText = text("İstanbul", 34, Color.WHITE, true);
        cityText.setGravity(Gravity.CENTER);
        cityText.setPadding(0, dp(12), 0, 0);
        cityText.setOnLongClickListener(v -> { showCityDialog(); return true; });
        cityText.setOnClickListener(v -> loadWeather(prefs.getString("city", "İstanbul")));
        content.addView(cityText, matchWrap());

        dateText = text("Güncelleniyor...", 15, 0xFFC7D2FE, false);
        dateText.setGravity(Gravity.CENTER);
        dateText.setPadding(0, dp(2), 0, dp(14));
        content.addView(dateText, matchWrap());

        LinearLayout hero = card(dp(18));
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(14), dp(18), dp(14), dp(18));
        content.addView(hero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        tempText = text("--°", 76, Color.WHITE, true);
        tempText.setGravity(Gravity.CENTER);
        tempText.setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= 26) {
            tempText.setAutoSizeTextTypeUniformWithConfiguration(56, 78, 1, TypedValue.COMPLEX_UNIT_SP);
        }
        hero.addView(tempText, matchWrap());

        weatherText = text("Hava durumu alınıyor", 24, 0xFFE0F2FE, true);
        weatherText.setGravity(Gravity.CENTER);
        weatherText.setPadding(0, dp(5), 0, dp(2));
        hero.addView(weatherText, matchWrap());

        feelsText = text("Hissedilen: --°", 16, 0xFFBAE6FD, false);
        feelsText.setGravity(Gravity.CENTER);
        hero.addView(feelsText, matchWrap());

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.VERTICAL);
        metrics.setPadding(0, dp(14), 0, 0);
        hero.addView(metrics, matchWrap());

        humidityText = metric("Nem", "--");
        windText = metric("Rüzgar", "--");
        rainText = metric("Yağış", "--");
        metrics.addView(humidityText);
        metrics.addView(windText);
        metrics.addView(rainText);

        TextView forecastTitle = text("5 GÜNLÜK TAHMİN", 14, 0xFF93C5FD, true);
        forecastTitle.setLetterSpacing(0.11f);
        forecastTitle.setPadding(0, dp(22), 0, dp(8));
        content.addView(forecastTitle, matchWrap());

        forecastContainer = new LinearLayout(this);
        forecastContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(forecastContainer, matchWrap());

        updateText = text("", 13, 0xFFA5B4FC, false);
        updateText.setGravity(Gravity.CENTER);
        updateText.setPadding(0, dp(16), 0, dp(5));
        content.addView(updateText, matchWrap());

        statusText = text("Şehir değiştirmek için şehir adına uzun bas. Yenilemek için dokun.", 12, 0xFFCBD5E1, false);
        statusText.setGravity(Gravity.CENTER);
        content.addView(statusText, matchWrap());

        root.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void loadWeather(String city) {
        if (!hasNetwork()) {
            statusText.setText("İnternet yok. Bağlantı gelince otomatik yenilenecek.");
            return;
        }
        statusText.setText("Veriler alınıyor...");
        cityText.setText(city);
        executor.execute(() -> {
            try {
                WeatherData data = fetchWeather(city);
                handler.post(() -> applyWeather(data));
            } catch (Exception e) {
                handler.post(() -> statusText.setText("Hava durumu alınamadı: " + e.getMessage()));
            }
        });
    }

    private WeatherData fetchWeather(String city) throws Exception {
        String q = URLEncoder.encode(city, StandardCharsets.UTF_8.name());
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + q + "&count=1&language=tr&format=json";
        JSONObject geo = new JSONObject(fetch(geoUrl));
        JSONArray results = geo.optJSONArray("results");
        if (results == null || results.length() == 0) throw new Exception("şehir bulunamadı");
        JSONObject first = results.getJSONObject(0);
        double lat = first.getDouble("latitude");
        double lon = first.getDouble("longitude");
        String displayCity = first.optString("name", city);
        String country = first.optString("country_code", "");
        if (!country.isEmpty()) displayCity += " / " + country;

        String api = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=" + lat + "&longitude=" + lon +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=5";
        JSONObject forecast = new JSONObject(fetch(api));
        JSONObject current = forecast.getJSONObject("current");
        JSONObject daily = forecast.getJSONObject("daily");

        WeatherData d = new WeatherData();
        d.city = displayCity;
        d.temp = current.getDouble("temperature_2m");
        d.feels = current.getDouble("apparent_temperature");
        d.humidity = current.getInt("relative_humidity_2m");
        d.wind = current.getDouble("wind_speed_10m");
        d.rain = current.getDouble("precipitation");
        d.code = current.getInt("weather_code");
        d.currentTime = current.optString("time", "");

        JSONArray days = daily.getJSONArray("time");
        JSONArray max = daily.getJSONArray("temperature_2m_max");
        JSONArray min = daily.getJSONArray("temperature_2m_min");
        JSONArray rainProb = daily.getJSONArray("precipitation_probability_max");
        JSONArray codes = daily.getJSONArray("weather_code");
        int count = Math.min(5, days.length());
        d.days = new String[count];
        d.max = new double[count];
        d.min = new double[count];
        d.rainProb = new int[count];
        d.codes = new int[count];
        for (int i = 0; i < count; i++) {
            d.days[i] = days.getString(i);
            d.max[i] = max.getDouble(i);
            d.min[i] = min.getDouble(i);
            d.rainProb[i] = rainProb.optInt(i, 0);
            d.codes[i] = codes.optInt(i, 0);
        }
        return d;
    }

    private void applyWeather(WeatherData d) {
        cityText.setText(d.city);
        tempText.setText(round(d.temp) + "°");
        weatherText.setText(icon(d.code) + "  " + weatherName(d.code));
        feelsText.setText("Hissedilen: " + round(d.feels) + "°C");
        humidityText.setText("Nem  •  %" + d.humidity);
        windText.setText("Rüzgar  •  " + one(d.wind) + " km/sa");
        rainText.setText("Yağış  •  " + one(d.rain) + " mm");
        dateText.setText(todayLine());
        updateText.setText("Son güncelleme: " + nowClock());
        statusText.setText("Canlı veri aktif. Yenilemek için şehir adına dokun.");

        forecastContainer.removeAllViews();
        for (int i = 0; i < d.days.length; i++) {
            forecastContainer.addView(forecastRow(d.days[i], d.codes[i], d.min[i], d.max[i], d.rainProb[i]));
        }
    }

    private TextView forecastRow(String isoDate, int code, double min, double max, int rain) {
        TextView row = text(formatDay(isoDate) + "  " + icon(code) + "  " + round(min) + "° / " + round(max) + "°  •  %" + rain,
                16, Color.WHITE, true);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setSingleLine(true);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x1FFFFFFF);
        gd.setCornerRadius(dp(16));
        gd.setStroke(dp(1), 0x22FFFFFF);
        row.setBackground(gd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);
        return row;
    }

    private void showCityDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);
        input.setText(prefs.getString("city", "İstanbul"));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Şehir seç")
                .setMessage("Örnek: İstanbul, Ankara, İzmir, London")
                .setView(input)
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String city = input.getText().toString().trim();
                    if (city.length() == 0) city = "İstanbul";
                    prefs.edit().putString("city", city).apply();
                    loadWeather(city);
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private String fetch(String urlString) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlString).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestMethod("GET");
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new Exception("sunucu hatası " + code);
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        c.disconnect();
        return sb.toString();
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        v.setIncludeFontPadding(true);
        return v;
    }

    private LinearLayout card(int radius) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x24FFFFFF);
        gd.setCornerRadius(radius);
        gd.setStroke(dp(1), 0x35FFFFFF);
        l.setBackground(gd);
        return l;
    }

    private TextView metric(String label, String value) {
        TextView v = text(label + "  •  " + value, 17, 0xFFF8FAFC, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    private boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private String round(double x) { return String.valueOf(Math.round(x)); }
    private String one(double x) { return String.format(tr, "%.1f", x); }

    private String todayLine() {
        return new SimpleDateFormat("EEEE, d MMMM yyyy", tr).format(new Date());
    }

    private String nowClock() {
        return new SimpleDateFormat("HH:mm", tr).format(new Date());
    }

    private String formatDay(String iso) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso);
            return new SimpleDateFormat("EEE d MMM", tr).format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    private String icon(int code) {
        if (code == 0) return "☀️";
        if (code == 1 || code == 2) return "🌤️";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 67) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌦️";
        if (code >= 85 && code <= 86) return "🌨️";
        if (code >= 95) return "⛈️";
        return "🌡️";
    }

    private String weatherName(int code) {
        switch (code) {
            case 0: return "Açık";
            case 1: return "Az bulutlu";
            case 2: return "Parçalı bulutlu";
            case 3: return "Kapalı";
            case 45:
            case 48: return "Sisli";
            case 51:
            case 53:
            case 55: return "Çiseleme";
            case 56:
            case 57: return "Donan çiseleme";
            case 61:
            case 63:
            case 65: return "Yağmurlu";
            case 66:
            case 67: return "Donan yağmur";
            case 71:
            case 73:
            case 75: return "Karlı";
            case 77: return "Kar taneleri";
            case 80:
            case 81:
            case 82: return "Sağanak";
            case 85:
            case 86: return "Kar sağanağı";
            case 95: return "Gök gürültülü";
            case 96:
            case 99: return "Dolu fırtınası";
            default: return "Hava durumu";
        }
    }

    private static class WeatherData {
        String city;
        double temp;
        double feels;
        int humidity;
        double wind;
        double rain;
        int code;
        String currentTime;
        String[] days;
        double[] max;
        double[] min;
        int[] rainProb;
        int[] codes;
    }
}
