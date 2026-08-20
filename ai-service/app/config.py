"""ai-service 配置：从 .env 读取，带默认值"""
import os
from dataclasses import dataclass, field
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


def _bool(v: str | None, default: bool = False) -> bool:
    if v is None:
        return default
    return v.strip().lower() in ("1", "true", "yes", "on")


@dataclass
class Settings:
    mysql_url: str = field(default_factory=lambda: os.getenv(
        "MYSQL_URL", "mysql+pymysql://worklog:CHANGE_ME@127.0.0.1:3306/worklog?charset=utf8mb4"))
    milvus_host: str = field(default_factory=lambda: os.getenv("MILVUS_HOST", "127.0.0.1"))
    milvus_port: int = field(default_factory=lambda: int(os.getenv("MILVUS_PORT", "19530")))
    milvus_collection: str = field(default_factory=lambda: os.getenv("MILVUS_COLLECTION", "geminix_docs"))
    embed_model: str = field(default_factory=lambda: os.getenv("EMBED_MODEL", "BAAI/bge-small-zh-v1.5"))
    embed_dim: int = field(default_factory=lambda: int(os.getenv("EMBED_DIM", "512")))
    chunk_size: int = field(default_factory=lambda: int(os.getenv("CHUNK_SIZE", "500")))


settings = Settings()
