from __future__ import annotations

import base64
import os
import time
from dataclasses import dataclass
from typing import Any

import jwt
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey, Ed25519PublicKey
from cryptography.hazmat.primitives.serialization import Encoding, PrivateFormat, PublicFormat, NoEncryption


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64url_decode(s: str) -> bytes:
    pad = "=" * (-len(s) % 4)
    return base64.urlsafe_b64decode((s + pad).encode("ascii"))


@dataclass(frozen=True)
class SigningKeys:
    kid: str
    private_key: Ed25519PrivateKey
    public_key: Ed25519PublicKey

    def public_jwk(self) -> dict[str, Any]:
        pub = self.public_key.public_bytes(Encoding.Raw, PublicFormat.Raw)
        return {"kty": "OKP", "crv": "Ed25519", "x": _b64url(pub), "kid": self.kid, "use": "sig", "alg": "EdDSA"}


def load_or_create_keys(data_dir: str) -> SigningKeys:
    os.makedirs(data_dir, exist_ok=True)
    priv_path = os.path.join(data_dir, "ed25519_private.key")
    pub_path = os.path.join(data_dir, "ed25519_public.key")
    kid_path = os.path.join(data_dir, "kid.txt")

    if os.path.exists(priv_path) and os.path.exists(pub_path) and os.path.exists(kid_path):
        with open(priv_path, "rb") as f:
            priv_raw = f.read()
        with open(pub_path, "rb") as f:
            pub_raw = f.read()
        with open(kid_path, "r", encoding="utf-8") as f:
            kid = f.read().strip()
        return SigningKeys(
            kid=kid,
            private_key=Ed25519PrivateKey.from_private_bytes(priv_raw),
            public_key=Ed25519PublicKey.from_public_bytes(pub_raw),
        )

    private_key = Ed25519PrivateKey.generate()
    public_key = private_key.public_key()
    kid = _b64url(os.urandom(8))

    priv_raw = private_key.private_bytes(Encoding.Raw, PrivateFormat.Raw, NoEncryption())
    pub_raw = public_key.public_bytes(Encoding.Raw, PublicFormat.Raw)
    with open(priv_path, "wb") as f:
        f.write(priv_raw)
    with open(pub_path, "wb") as f:
        f.write(pub_raw)
    with open(kid_path, "w", encoding="utf-8") as f:
        f.write(kid)

    return SigningKeys(kid=kid, private_key=private_key, public_key=public_key)


def issue_access_jwt(*, keys: SigningKeys, claims: dict[str, Any], ttl_seconds: int) -> str:
    now = int(time.time())
    payload = dict(claims)
    payload["iat"] = now
    payload["nbf"] = now - 1
    payload["exp"] = now + ttl_seconds
    headers = {"kid": keys.kid, "alg": "EdDSA", "typ": "JWT"}
    return jwt.encode(payload, keys.private_key, algorithm="EdDSA", headers=headers)


def verify_access_jwt(*, token: str, public_key: Ed25519PublicKey) -> dict[str, Any]:
    return jwt.decode(token, public_key, algorithms=["EdDSA"], options={"require": ["exp", "iat"]})

