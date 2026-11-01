package com.kestrel7.studio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    // Ganti dengan URL HTTPS backend milik Anda setelah backend dideploy.
    // Jangan pernah menaruh OPENAI_API_KEY di sini.
    private static final String API_BASE_URL = "https://YOUR-BACKEND-HOST";
    private Uri selectedImage;
    private ImageView preview;
    private EditText prompt;
    private Spinner categorySpinner;
    private CheckBox adultCheck;
    private Button processButton, saveButton;
    private Bitmap resultBitmap;

    private final ActivityResultLauncher<String> picker =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImage = uri;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    preview.setImageBitmap(bmp);
                    resultBitmap = null;
                    saveButton.setVisibility(View.GONE);
                    status.setText("Foto siap diproses.");
                } catch (Exception e) { status.setText("Tidak dapat membaca foto."); }
            }
        });

    private TextView status;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        buildUi();
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        page.setBackgroundColor(0xFF0D1728);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("KESTREL-7 STUDIO");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(8));
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("AI IMAGE EDITOR");
        subtitle.setTextColor(0xFF79D7C7);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle);

        preview = new ImageView(this);
        preview.setBackgroundColor(0xFF1C2A40);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(-1, dp(280));
        imageLp.setMargins(0, dp(22), 0, dp(16));
        page.addView(preview, imageLp);

        Button choose = makeButton("PILIH FOTO");
        page.addView(choose);
        choose.setOnClickListener(v -> picker.launch("image/*"));

        adultCheck = new CheckBox(this);
        adultCheck.setText("Saya berusia 18 tahun atau lebih dan memahami aturan penggunaan.");
        adultCheck.setTextColor(0xFFFFFFFF);
        adultCheck.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF79D7C7));
        LinearLayout.LayoutParams ageLp = new LinearLayout.LayoutParams(-1, -2);
        ageLp.setMargins(0, dp(12), 0, dp(4));
        page.addView(adultCheck, ageLp);

        TextView categoryLabel = new TextView(this);
        categoryLabel.setText("MODE KREATIF");
        categoryLabel.setTextColor(0xFFFFFFFF);
        categoryLabel.setPadding(0, dp(12), 0, dp(6));
        page.addView(categoryLabel);

        categorySpinner = new Spinner(this);
        String[] categories = {"Fashion Editorial", "Swimwear & Lingerie", "Artistic Portrait", "Adult Creative (sesuai kebijakan layanan AI)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        page.addView(categorySpinner);

        TextView label = new TextView(this);
        label.setText("INSTRUKSI EDIT");
        label.setTextColor(0xFFFFFFFF);
        label.setPadding(0, dp(18), 0, dp(8));
        page.addView(label);

        prompt = new EditText(this);
        prompt.setTextColor(0xFFFFFFFF);
        prompt.setHintTextColor(0xFF9BA9BD);
        prompt.setHint("Contoh: Ganti pakaian menjadi jaket denim biru. Pertahankan wajah, pose, dan latar.");
        prompt.setMinLines(3);
        prompt.setGravity(Gravity.TOP);
        prompt.setPadding(dp(12), dp(12), dp(12), dp(12));
        prompt.setBackgroundColor(0xFF1C2A40);
        page.addView(prompt, new LinearLayout.LayoutParams(-1, dp(120)));

        processButton = makeButton("PROSES GAMBAR");
        LinearLayout.LayoutParams processLp = new LinearLayout.LayoutParams(-1, dp(52));
        processLp.setMargins(0, dp(18), 0, dp(8));
        page.addView(processButton, processLp);
        processButton.setOnClickListener(v -> processImage());

        status = new TextView(this);
        status.setText("Pilih foto dan tulis instruksi untuk memulai.");
        status.setTextColor(0xFFD4DCE8);
        status.setPadding(0, dp(8), 0, dp(8));
        page.addView(status);

        saveButton = makeButton("SIMPAN HASIL");
        page.addView(saveButton);
        saveButton.setVisibility(View.GONE);
        saveButton.setOnClickListener(v -> saveResult());

        setContentView(scroll);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF176B69));
        return b;
    }

    private void processImage() {
        if (!adultCheck.isChecked()) { status.setText("Konfirmasi usia 18+ terlebih dahulu."); return; }
        if (selectedImage == null) { status.setText("Pilih foto terlebih dahulu."); return; }
        String instruction = "[" + categorySpinner.getSelectedItem().toString() + "] " + prompt.getText().toString().trim();
        if (instruction.trim().isEmpty()) { status.setText("Tulis instruksi edit terlebih dahulu."); return; }
        if (API_BASE_URL.contains("YOUR-BACKEND-HOST")) {
            status.setText("Backend belum disambungkan. Deploy backend lalu isi API_BASE_URL di aplikasi.");
            return;
        }
        processButton.setEnabled(false);
        status.setText("Mengirim foto ke AI. Mohon tunggu...");
        new Thread(() -> {
            try {
                byte[] imageBytes;
                try (InputStream in = getContentResolver().openInputStream(selectedImage);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    imageBytes = out.toByteArray();
                }
                OkHttpClient client = new OkHttpClient.Builder()
                    .callTimeout(180, TimeUnit.SECONDS).build();
                RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("prompt", instruction)
                    .addFormDataPart("image", "input.jpg",
                        RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                    .build();
                Request request = new Request.Builder()
                    .url(API_BASE_URL + "/edit").post(body).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Server error " + response.code() + ": " + response.body().string());
                    JSONObject json = new JSONObject(response.body().string());
                    String data = json.getString("image_base64");
                    byte[] decoded = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    runOnUiThread(() -> {
                        resultBitmap = bmp; preview.setImageBitmap(bmp);
                        status.setText("Selesai! Hasil edit siap disimpan.");
                        saveButton.setVisibility(View.VISIBLE);
                        processButton.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Gagal memproses: " + e.getMessage());
                    processButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void saveResult() {
        if (resultBitmap == null) return;
        try {
            String name = "Kestrel7_" + System.currentTimeMillis() + ".png";
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kestrel7");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            status.setText("Hasil disimpan di Pictures/Kestrel7.");
        } catch (Exception e) { status.setText("Gagal menyimpan: " + e.getMessage()); }
    }
}
