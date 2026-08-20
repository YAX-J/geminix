"""geminix ai-service：个人知识库向量检索（FastAPI）
接口：
  GET  /api/ai/health        健康检查
  POST /api/ai/sync          全量同步 MySQL → Milvus
  GET  /api/ai/search?q=..   语义检索（返回来源元数据）
"""
import logging

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware

from . import milvus_client, sync
from .embedder import embed_query
from .schemas import SearchResp

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

app = FastAPI(title="geminix ai-service", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/ai/health")
def health():
    return {"status": "ok"}


@app.post("/api/ai/sync")
def sync_endpoint():
    """全量重建向量库（数据量小，直接 drop + 重灌）"""
    return sync.sync_all()


@app.get("/api/ai/search", response_model=SearchResp)
def search_endpoint(q: str = Query(..., min_length=1), k: int = Query(5, ge=1, le=20)):
    vec = embed_query(q)
    hits = milvus_client.search(vec, k=k)
    return SearchResp(query=q, hits=hits)
