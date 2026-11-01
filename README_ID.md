# Kestrel-7 Studio — versi web

Aplikasi web responsif untuk dibuka melalui browser HP. Ini **paket aplikasi yang siap di-deploy**, bukan situs publik yang sudah online.

## Cara paling praktis: deploy di Render
1. Buat akun GitHub dan Render, jika belum punya.
2. Unggah isi folder proyek ini ke repositori GitHub baru.
3. Di Render, pilih **New + → Blueprint** dan hubungkan repositori tersebut. Render akan membaca `render.yaml`.
4. Saat diminta, masukkan `OPENAI_API_KEY` sebagai environment variable rahasia di pengaturan layanan Render. Jangan masukkan API key ke HTML atau unggah ke repositori.
5. Deploy dan tunggu sampai selesai. Render akan memberi alamat `https://...onrender.com` yang bisa dibuka di browser Android.

Alternatif: buat Web Service di Render, Build Command `pip install -r requirements.txt`, Start Command `gunicorn app:app`, Runtime Python. Tambahkan `OPENAI_API_KEY` di Environment.

## Syarat
- API key OpenAI yang valid, akses ke endpoint/model gambar, koneksi internet, dan penagihan API bila berlaku.
- API key disimpan sebagai variabel lingkungan di server, bukan di browser.
- Hosting gratis dapat tidur saat tidak dipakai dan memiliki batasan. Penggunaan API gambar bisa berbayar.
- Foto dikirim ke server dan API gambar untuk diproses. Jangan gunakan foto tanpa hak/izin yang sesuai.
- Aplikasi tidak melewati moderasi atau aturan penyedia API.
- Paket ini belum di-deploy ke situs publik. Saya tidak dapat membuat URL publik aktif tanpa akses ke akun hosting dan konfigurasi pemilik.
