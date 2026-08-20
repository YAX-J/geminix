"""Pydantic 模型"""
from pydantic import BaseModel


class SearchHit(BaseModel):
    score: float
    type: str
    ref_id: str
    ref_date: str = ""
    title: str = ""
    project: str = ""
    text: str = ""


class SearchResp(BaseModel):
    query: str
    hits: list[SearchHit]
