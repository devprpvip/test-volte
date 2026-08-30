#!/usr/bin/env python3
"""
Pack Sec Auto Clear thành .crx (CRX3) để Chrome tự lưu vào profile.
Sau khi cài .crx, bạn xóa file gốc/thư mục tải về cũng không sao —
Chrome đã copy extension vào User Data/Default/Extensions/<id>/.

Dùng: python3 pack.py
Yêu cầu: openssl có sẵn (để tạo key) hoặc đã có sec-auto-clear.pem
"""
import os, struct, hashlib, zipfile, pathlib, subprocess, sys

EXT_DIR = pathlib.Path(__file__).parent
PEM = EXT_DIR / "sec-auto-clear.pem"
CRX = EXT_DIR / "sec-auto-clear.crx"
ZIP_TMP = EXT_DIR / "_tmp.zip"

def gen_key():
    if PEM.exists():
        print(f"[pack] dùng key có sẵn: {PEM}")
        return
    print("[pack] tạo private key 2048...")
    # thử dùng openssl
    try:
        subprocess.check_call(["openssl", "genrsa", "-out", str(PEM), "2048"])
        print(f"[pack] đã tạo {PEM} - GIỮ FILE NÀY để update sau này!")
    except Exception as e:
        print(f"[pack] không có openssl hoặc lỗi: {e}")
        print("[pack] tạo key bằng cryptography (nếu cài)? thử fallback...")
        try:
            from cryptography.hazmat.primitives.asymmetric import rsa
            from cryptography.hazmat.primitives import serialization
            key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
            PEM.write_bytes(key.private_bytes(
                serialization.Encoding.PEM,
                serialization.PrivateFormat.TraditionalOpenSSL,
                serialization.NoEncryption()
            ))
            print(f"[pack] đã tạo {PEM} bằng cryptography")
        except Exception as e2:
            print(f"[pack] FAIL: {e2}")
            sys.exit(1)

def create_zip():
    # zip toàn bộ file cần thiết, loại trừ pack.py, *.pem, *.crx, _tmp
    exclude = {"pack.py", "sec-auto-clear.pem", "sec-auto-clear.crx", "_tmp.zip", ".DS_Store", "README.md"}
    with zipfile.ZipFile(ZIP_TMP, "w", zipfile.ZIP_DEFLATED) as z:
        for p in sorted(EXT_DIR.rglob("*")):
            if p.is_dir(): continue
            if p.name in exclude: continue
            if p.suffix in {".pyc", ".pyo", ".crx", ".pem", ".zip"}: continue
            if ".git" in p.parts: continue
            if "__pycache__" in p.parts: continue
            arc = p.relative_to(EXT_DIR).as_posix()
            z.write(p, arc)
            print(f"  + {arc}")
    print(f"[pack] zip: {ZIP_TMP} ({ZIP_TMP.stat().st_size} bytes)")

def create_webstore_zip():
    # Zip upload Chrome Web Store (dashboard chỉ nhận .zip, KHÔNG nhận .crx)
    # Giữ README.md vì store cho phép; loại key/crx/pack script.
    out = EXT_DIR / "sec-auto-clear-webstore.zip"
    exclude = {"pack.py", "sec-auto-clear.pem", "sec-auto-clear.crx",
               "sec-auto-clear-webstore.zip", "_tmp.zip", ".DS_Store"}
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for p in sorted(EXT_DIR.rglob("*")):
            if p.is_dir(): continue
            if p.name in exclude: continue
            if p.suffix in {".pyc", ".pyo", ".crx", ".pem", ".zip"}: continue
            if ".git" in p.parts: continue
            if "__pycache__" in p.parts: continue
            z.write(p, p.relative_to(EXT_DIR).as_posix())
    print(f"[pack] webstore zip: {out} ({out.stat().st_size} bytes) - upload tại https://chrome.google.com/webstore/devconsole")

def make_crx():
    # Đọc private key và tạo public key DER
    # Dùng openssl để lấy public key DER
    pub_der = None
    # openssl rsa -in key.pem -pubout -outform DER
    try:
        pub_der = subprocess.check_output(["openssl", "rsa", "-in", str(PEM), "-pubout", "-outform", "DER"])
        print(f"[pack] public key DER: {len(pub_der)} bytes")
    except Exception as e:
        print(f"[pack] lỗi lấy public key: {e}")
        sys.exit(1)

    zip_data = ZIP_TMP.read_bytes()

    # Tạo header CRX3 theo spec: https://developer.chrome.com/docs/extensions/how-to/crx
    # CRX3 structure: magic(4) "Cr24" + version(4) 3 + header_size(4) + header (protobuf) + zip
    # header protobuf: CrxFileHeader { sha256_with_rsa (repeated) + sha256_with_ec... + signed_header_data }
    # Để đơn giản, ta dùng cách thủ công: tạo SignedData protobuf bằng tay (field numbers)
    # Thay vì implement protobuf đầy đủ, ta dùng crx3 đơn giản nhất: chỉ cần signed_header_data chứa crx_id
    # Nhưng cách dễ hơn: dùng cấu trúc Chrome chấp nhận cho self-hosted với header rỗng + signature?
    # Thực tế Chrome yêu cầu signature hợp lệ. Ta sẽ tạo theo spec minimal bằng python struct + openssl sign.

    # Cách đơn giản nhất: dùng openssl để sign zip_data với private key, rồi tự build header protobuf thủ công.
    # Reference: https://github.com/ChromeDev/crx-packager

    # Tạo crx_id theo Chrome: hex của 16 bytes đầu SHA256(pubkey) map 0->a, 1->b...
    # Ví dụ sha hex "ab12..." -> "bc..."
    hex_id = hashlib.sha256(pub_der).hexdigest()[:32]
    crx_id_correct = "".join(chr(int(c,16)+ord('a')) for c in hex_id)
    print(f"[pack] crx id (dự kiến): {crx_id_correct}")

    # Build SignedData trước để ký
    def varint(n):
        out=b""
        while n>127:
            out+=bytes([(n&0x7F)|0x80])
            n>>=7
        out+=bytes([n])
        return out
    def field(num, wire, data):
        return varint((num<<3)|wire)+data
    def bytes_field(num, b):
        return field(num,2,varint(len(b))+b)
    # SignedData: crx_id = field 1, bytes (16 bytes raw)
    crx_id_raw = hashlib.sha256(pub_der).digest()[:16]
    signed_data = bytes_field(1, crx_id_raw)

    # Signature phải ký lên signed_data (theo CRX3 spec), KHÔNG phải zip
    # Dùng openssl dgst -sha256 -sign
    try:
        sig = subprocess.check_output(["openssl", "dgst", "-sha256", "-binary", "-sign", str(PEM)], input=signed_data)
        print(f"[pack] signature (over signed_data): {len(sig)} bytes")
    except Exception as e:
        print(f"[pack] sign fail: {e}")
        sig = subprocess.check_output(["openssl", "pkeyutl", "-sign", "-inkey", str(PEM), "-pkeyopt", "digest:sha256", "-rawin", "-in", "/dev/stdin"], input=signed_data)
    
    # Build CRX3 header protobuf
    # Header = CrxFileHeader { sha256_with_rsa: [{public_key, signature}], signed_header_data: bytes }
    # CrxFileHeader: signed_header_data = field 10000 ?
    # Theo spec crx3.proto: signed_header_data = field 1? Thực tế header là: 
    # message CrxFileHeader { repeated AsymmetricKeyProof sha256_with_rsa = 2; bytes signed_header_data = 10000; }
    # AsymmetricKeyProof { bytes public_key = 1; bytes signature = 2; }
    # Thử build đúng field numbers
    proof = bytes_field(1, pub_der) + bytes_field(2, sig)
    proof_wrapped = bytes_field(2, proof)  # sha256_with_rsa = 2
    header = proof_wrapped + bytes_field(10000, signed_data)
    header_size = len(header)
    # CRX file: "Cr24" + version 3 (LE32) + header_size (LE32) + header + zip
    crx_data = b"Cr24" + struct.pack("<III", 3, header_size, 0)[:8]  # chỉ cần 2 số? Thực ra là version + header_size
    # Sửa: pack 2 số LE32
    crx_data = b"Cr24" + struct.pack("<I", 3) + struct.pack("<I", header_size) + header + zip_data
    CRX.write_bytes(crx_data)
    print(f"[pack] CRX tạo: {CRX} ({CRX.stat().st_size} bytes)")
    print(f"[pack] ID: {crx_id_correct}")
    print("[pack] Xong! Kéo file .crx vào chrome://extensions (Developer mode ON) để cài.")
    print("[pack] Sau khi cài, Chrome copy vào User Data/Default/Extensions/<id>/ -> xóa file gốc vẫn OK.")
    print("[pack] LƯU GIỮ file .pem để update sau này (cùng ID).")

if __name__=="__main__":
    gen_key()
    create_zip()
    make_crx()
    create_webstore_zip()
    # dọn tmp
    try: ZIP_TMP.unlink()
    except: pass
