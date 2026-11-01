
import base64
import os

import httpx
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Kestrel7 Studio Backend", version="3.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    return {"status": "ok", "service": "Kestrel7 Studio"}

@app.post("/edit")
async def edit_image(
    prompt: str = Form(...),
    image: UploadFile = File(...)
):
    api_key = os.getenv("POLLINATIONS_API_KEY")

    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="POLLINATIONS_API_KEY belum disetel."
        )

    if not prompt.strip():
        raise HTTPException(status_code=400, detail="Prompt kosong.")

    content = await image.read()

    if not content:
        raise HTTPException(status_code=400, detail="Foto kosong.")

    if len(content) > 20 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Foto maksimal 20 MB.")

    mime = image.content_type or "image/jpeg"

    if mime not in ("image/jpeg", "image/png", "image/webp"):
        raise HTTPException(status_code=415, detail="Format foto tidak didukung.")

    headers = {"Authorization": f"Bearer {api_key}"}

    files = {
        "image": (image.filename or "photo.jpg", content, mime)
    }

    data = {
        "model": "kontext",
        "prompt": prompt.strip(),
        "size": "1024x1024"
    }

    async with httpx.AsyncClient(timeout=240.0) as client:
        try:
            response = await client.post(
                "https://gen.pollinations.ai/v1/images/edits",
                headers=headers,
                files=files,
                data=data
            )
        except httpx.RequestError:
            raise HTTPException(
                status_code=502,
                detail="Tidak dapat menghubungi Pollinations."
            )

    if response.status_code >= 400:
        raise HTTPException(
            status_code=response.status_code,
            detail="Pollinations gagal memproses foto. Periksa API Key, saldo Pollen, dan model."
        )

    try:
        result = response.json()
        image_b64 = result["data"][0]["b64_json"]
        base64.b64decode(image_b64, validate=True)
    except Exception:
        raise HTTPException(
            status_code=502,
            detail="Respons AI tidak berisi gambar yang valid."
        )

    return {"image_base64": image_b64}
