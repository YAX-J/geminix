"""Milvus 集合管理与检索（MilvusClient API，兼容 Milvus v3.x）
集合 geminix_docs：512 维 FLOAT_VECTOR，COSINE + IVF_FLAT
"""
import logging

from pymilvus import DataType, MilvusClient

from .config import settings

log = logging.getLogger(__name__)

_client: MilvusClient | None = None


def get_client() -> MilvusClient:
    global _client
    if _client is None:
        _client = MilvusClient(uri=f"tcp://{settings.milvus_host}:{settings.milvus_port}")
    return _client


def ensure_collection() -> None:
    name = settings.milvus_collection
    client = get_client()
    if client.has_collection(name):
        return

    schema = MilvusClient.create_schema(auto_id=False, enable_dynamic_field=False)
    schema.add_field(field_name="id", datatype=DataType.VARCHAR, is_primary=True, max_length=64)
    schema.add_field(field_name="embedding", datatype=DataType.FLOAT_VECTOR, dim=settings.embed_dim)
    schema.add_field(field_name="text", datatype=DataType.VARCHAR, max_length=2048)
    schema.add_field(field_name="type", datatype=DataType.VARCHAR, max_length=16)     # report / issue
    schema.add_field(field_name="ref_id", datatype=DataType.VARCHAR, max_length=32)
    schema.add_field(field_name="ref_date", datatype=DataType.VARCHAR, max_length=16)
    schema.add_field(field_name="title", datatype=DataType.VARCHAR, max_length=256)
    schema.add_field(field_name="project", datatype=DataType.VARCHAR, max_length=64)

    index_params = client.prepare_index_params()
    index_params.add_index(
        field_name="embedding",
        index_type="IVF_FLAT",
        metric_type="COSINE",
        params={"nlist": 128},
    )

    client.create_collection(
        collection_name=name,
        schema=schema,
        index_params=index_params,
    )
    log.info("collection %s created (dim=%d)", name, settings.embed_dim)


def rebuild(data: list[dict]) -> int:
    """清空并重建集合（数据量小，全量重建最简单可靠）"""
    name = settings.milvus_collection
    client = get_client()
    if client.has_collection(name):
        client.drop_collection(name)
    ensure_collection()
    if not data:
        return 0
    client.insert(collection_name=name, data=data)
    return len(data)


def search(vector: list[float], k: int = 5) -> list[dict]:
    name = settings.milvus_collection
    client = get_client()
    ensure_collection()
    res = client.search(
        collection_name=name,
        data=[vector],
        limit=k,
        output_fields=["type", "ref_id", "ref_date", "title", "project", "text"],
        search_params={"metric_type": "COSINE"},
    )
    hits = []
    for h in res[0]:
        entity = h.get("entity", {})
        hits.append({
            "score": round(float(h.get("distance", 0)), 4),
            "type": entity.get("type", ""),
            "ref_id": entity.get("ref_id", ""),
            "ref_date": entity.get("ref_date", ""),
            "title": entity.get("title", ""),
            "project": entity.get("project", ""),
            "text": entity.get("text", ""),
        })
    return hits
