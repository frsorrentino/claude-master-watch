"""Sintesi con Gemini: python3 voice.py <testo.txt> <voce> "<stile a parole>" <uscita.wav>
La chiave viene da ~/.claude/fable-director/cross-family.json e non si stampa mai."""
import base64, json, os, re, sys, urllib.request, wave
from pathlib import Path

MODEL = os.environ.get("TTS_MODEL", "gemini-3.1-flash-tts-preview")
cfg = json.loads((Path.home() / ".claude/fable-director/cross-family.json").read_text())["providers"]["gemini"]
key = cfg.get("api_key") or os.environ[cfg["api_key_env"]]
text, voice, style, out = Path(sys.argv[1]).read_text().strip(), sys.argv[2], sys.argv[3], sys.argv[4]
body = {"contents": [{"parts": [{"text": f"{style}: {text}"}]}],
        "generationConfig": {"responseModalities": ["AUDIO"], "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": voice}}}}}
req = urllib.request.Request(f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent",
                             data=json.dumps(body).encode(), headers={"Content-Type": "application/json", "x-goog-api-key": key})
part = json.loads(urllib.request.urlopen(req, timeout=120).read())["candidates"][0]["content"]["parts"][0]["inlineData"]
m = re.search(r"rate=(\d+)", part["mimeType"]); rate = int(m.group(1)) if m else 24000          # es. «audio/L16;codec=pcm;rate=24000; channels=1»
with wave.open(out, "wb") as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate); w.writeframes(base64.b64decode(part["data"]))
print(out, part["mimeType"])
