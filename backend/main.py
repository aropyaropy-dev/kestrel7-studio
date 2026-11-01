import base64
import os
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
import httpx

app = FastAPI(title="Kestrel-7 Image Backend", version="2.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Batasi ke domain aplikasi/layanan Anda saat deploy.
    allow_credentials=False,
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    return {"status": "ok", "service": "Kestrel-7"}

@app.post("/edit")
async def edit_image(prompt: str = Form(...), image: UploadFile = File(...)):
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=503, detail="OPENAI_API_KEY belum disetel di server.")
    if not prompt.strip():
        raise HTTPException(status_code=400, detail="Instruksi edit tidak boleh kosong.")
    content = await image.read()
    if not content:
        raise HTTPException(status_code=400, detail="File gambar kosong.")
    if len(content) > 20 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Ukuran gambar maksimal 20 MB.")
    mime = image.content_type or "image/jpeg"
    if mime not in ("image/jpeg", "image/png", "image/webp"):
        raise HTTPException(status_code=415, detail="Format gambar harus JPEG, PNG, atau WebP.")

    # OpenAI Images API image edits endpoint.
    files = {"image": (image.filename or "input.jpg", content, mime)}
    data = {"model": "gpt-image-1", "prompt": prompt.strip(), "size": "auto"}
    headers = {"Authorization": f"Bearer {api_key}"}
    async with httpx.AsyncClient(timeout=180.0) as client:
        try:
            response = await client.post(
                "https://api.openai.com/v1/images/edits",
                headers=headers,
                data=data,
                files=files,
            )
        except httpx.RequestError as exc:
            raise HTTPException(status_code=502, detail=f"Tidak dapat menghubungi layanan AI: {exc}")
    if response.status_code >= 400:
        try:
            detail = response.json().get("error", {}).get("message", "Permintaan AI gagal.")
        except Exception:
            detail = "Permintaan AI gagal."
        raise HTTPException(status_code=response.status_code, detail=detail)

    result = response.json()
    try:
        image_b64 = result["data"][0]["b64_json"]
    except (KeyError, IndexError, TypeError):
        raise HTTPException(status_code=502, detail="Respons AI tidak berisi gambar.")
    return {"image_base64": image_b64}
