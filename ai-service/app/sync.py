"""同步管道：MySQL 日报/问题 → 分块 → embedding → Milvus 全量重建"""
import logging

from . import db, milvus_client
from .config import settings
from .embedder import embed_texts

log = logging.getLogger(__name__)


def _chunk_text(text: str, size: int) -> list[str]:
    """按长度切块（保留语义，避免截断单词；中文直接按字符切）"""
    text = (text or "").strip()
    if not text:
        return []
    if len(text) <= size:
        return [text]
    return [text[i:i + size] for i in range(0, len(text), size)]


def build_chunks() -> list[dict]:
    """构造所有向量化条目：{id, text, type, ref_id, ref_date, title, project}"""
    chunks: list[dict] = []

    for r in db.fetch_reports():
        tasks = r.get("tasks") or []
        if not isinstance(tasks, list):
            continue
        for i, task in enumerate(tasks):
            text = f"[{r['report_date']}] {r['title']}: {task}".strip()
            for seg in _chunk_text(text, settings.chunk_size):
                chunks.append({
                    "id": f"report_{r['id']}_{i}",
                    "text": seg[:2048],
                    "type": "report",
                    "ref_id": str(r["id"]),
                    "ref_date": str(r["report_date"])[:10],
                    "title": (r["title"] or "")[:256],
                    "project": (r.get("project") or "")[:64],
                })

    for iss in db.fetch_issues():
        body = f"问题：{iss['title']}\n描述：{iss.get('description') or ''}"
        if iss.get("solution"):
            body += f"\n方案：{iss['solution']}"
        for i, seg in enumerate(_chunk_text(body, settings.chunk_size)):
            chunks.append({
                "id": f"issue_{iss['id']}_{i}",
                "text": seg[:2048],
                "type": "issue",
                "ref_id": str(iss["id"]),
                "ref_date": "",
                "title": (iss["title"] or "")[:256],
                "project": (iss.get("project") or "")[:64],
            })

    return chunks


def sync_all() -> dict:
    """全量重建向量库：返回 {chunks, vectors, inserted}"""
    chunks = build_chunks()
    texts = [c["text"] for c in chunks]
    vectors = embed_texts(texts)
    data = [
        {**c, "embedding": v}
        for c, v in zip(chunks, vectors)
    ]
    inserted = milvus_client.rebuild(data)
    log.info("sync done: chunks=%d inserted=%d", len(chunks), inserted)
    return {"chunks": len(chunks), "inserted": inserted}
