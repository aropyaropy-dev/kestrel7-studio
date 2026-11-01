import os
import base64
from io import BytesIO
from flask import Flask, render_template, request, send_file, jsonify
from openai import OpenAI

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 20 * 1024 * 1024  # 20 MB upload limit

@app.get("/")
def home():
    return render_template("index.html")

@app.post("/edit")
def edit_image():
    api_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not api_key:
        return jsonify(error="API belum dikonfigurasi oleh pemilik aplikasi. Tambahkan OPENAI_API_KEY pada environment server."), 503

    uploaded = request.files.get("image")
    prompt = (request.form.get("prompt") or "").strip()
    if not uploaded or not uploaded.filename:
        return jsonify(error="Pilih foto terlebih dahulu."), 400
    if not prompt:
        return jsonify(error="Tulis instruksi edit terlebih dahulu."), 400

    mime = uploaded.mimetype or "image/jpeg"
    if not mime.startswith("image/"):
        return jsonify(error="File yang dipilih harus berupa gambar."), 400

    try:
        client = OpenAI(api_key=api_key)
        raw = uploaded.read()
        result = client.images.edit(
            model=os.environ.get("OPENAI_IMAGE_MODEL", "gpt-image-1"),
            image=(uploaded.filename, BytesIO(raw), mime),
            prompt=prompt,
        )
        b64 = result.data[0].b64_json
        output = base64.b64decode(b64)
        return send_file(BytesIO(output), mimetype="image/png", as_attachment=True, download_name="kestrel7-edited.png")
    except Exception as exc:
        # Avoid returning internal stack traces or environment details to the browser.
        message = str(exc)
        if len(message) > 500:
            message = message[:500] + "…"
        return jsonify(error="Proses edit gagal. Periksa API key, akses model gambar, kuota/penagihan API, dan format foto. Detail: " + message), 502

if __name__ == "__main__":
    port = int(os.environ.get("PORT", "5000"))
    app.run(host="0.0.0.0", port=port)
