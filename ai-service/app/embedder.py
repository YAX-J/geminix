"""embedding 封装：sentence-transformers 本地模型（bge 系列）"""
from functools import lru_cache

from .config import settings


@lru_cache(maxsize=1)
def get_embedder():
    from sentence_transformers import SentenceTransformer
    # bge 模型建议加查询指令前缀以提升检索质量
    return SentenceTransformer(settings.embed_model)


def embed_texts(texts: list[str]) -> list[list[float]]:
    """批量向量化，返回归一化向量列表"""
    if not texts:
        return []
    model = get_embedder()
    vecs = model.encode(texts, normalize_embeddings=True, show_progress_bar=False)
    return [v.tolist() for v in vecs]


def embed_query(text: str) -> list[float]:
    """单条查询向量（bge 检索指令前缀）"""
    prefixed = f"为这个句子生成表示以用于检索相关文章：{text}"
    return embed_texts([prefixed])[0]
