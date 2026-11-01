package com.kestrel7.studio;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 42;
    private ImageView preview;
    private TextView status;
    private EditText prompt;
    private Uri selectedImage;

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView label(String text, int size) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(Color.WHITE); return t;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); return b;
    }
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(21,21,29));
        getWindow().setNavigationBarColor(Color.rgb(21,21,29));
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.rgb(21,21,29));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(26),dp(22),dp(24));
        scroll.addView(root);
        TextView title = label("KESTREL-7", 30); title.setGravity(Gravity.CENTER); title.setTypeface(null, 1); root.addView(title);
        TextView sub = label("STUDIO  •  AI PHOTO WORKSPACE", 12); sub.setGravity(Gravity.CENTER); sub.setPadding(0,dp(5),0,dp(24)); root.addView(sub);
        preview = new ImageView(this); preview.setMinimumHeight(dp(210)); preview.setBackgroundColor(Color.rgb(38,38,49)); preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(230)));
        Button choose = button("1. Pilih foto dari HP"); root.addView(choose); choose.setOnClickListener(v -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, PICK_IMAGE); });
        TextView pLabel = label("2. Tulis instruksi edit", 16); pLabel.setPadding(0,dp(16),0,dp(6)); root.addView(pLabel);
        prompt = new EditText(this); prompt.setTextColor(Color.WHITE); prompt.setHintTextColor(Color.LTGRAY); prompt.setHint("Contoh: ganti pakaian menjadi jaket denim biru"); prompt.setGravity(Gravity.TOP|Gravity.START); prompt.setMinLines(3); prompt.setPadding(dp(12),dp(12),dp(12),dp(12)); prompt.setBackgroundColor(Color.rgb(38,38,49)); root.addView(prompt);
        Button demo = button("3. Siapkan instruksi"); root.addView(demo); demo.setOnClickListener(v -> {
            if (selectedImage == null) { Toast.makeText(this,"Pilih foto terlebih dahulu.",Toast.LENGTH_SHORT).show(); return; }
            if (prompt.getText().toString().trim().isEmpty()) { Toast.makeText(this,"Tulis instruksi edit terlebih dahulu.",Toast.LENGTH_SHORT).show(); return; }
            status.setText("Foto dan instruksi siap. Untuk menghasilkan edit AI sungguhan, aplikasi perlu dihubungkan ke layanan AI yang aman dengan server/API key.");
        });
        status = label("Demo awal: pemilihan foto dan penyusunan instruksi berfungsi. Generasi AI belum diaktifkan.", 14); status.setPadding(0,dp(14),0,0); root.addView(status);
        setContentView(scroll);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImage = data.getData();
            try { preview.setImageURI(selectedImage); status.setText("Foto dipilih. Tulis instruksi edit di bawah."); }
            catch (Exception e) { Toast.makeText(this,"Foto tidak dapat dibuka.",Toast.LENGTH_SHORT).show(); }
        }
    }
}
