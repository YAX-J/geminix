"""MySQL 读取：日报(report) / 问题(issue)，供向量化同步使用"""
import json

from sqlalchemy import create_engine, text

from .config import settings

_engine = None


def get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(settings.mysql_url, pool_pre_ping=True, pool_recycle=3600)
    return _engine


def _parse_json_list(v):
    """tasks/tags 等 JSON 列：pymysql 返回字符串，解析为 list"""
    if v is None:
        return []
    if isinstance(v, list):
        return v
    try:
        return json.loads(v)
    except (ValueError, TypeError):
        return []


def fetch_reports() -> list[dict]:
    """全部日报：id/date/title/tasks/project/tags"""
    with get_engine().connect() as conn:
        rows = conn.execute(text(
            "SELECT id, report_date, title, tasks, project, tags FROM report ORDER BY report_date"
        )).mappings().all()
    result = []
    for r in rows:
        d = dict(r)
        d["tasks"] = _parse_json_list(d.get("tasks"))
        d["tags"] = _parse_json_list(d.get("tags"))
        result.append(d)
    return result


def fetch_issues() -> list[dict]:
    """全部问题：id/title/description/tag/project/status"""
    with get_engine().connect() as conn:
        rows = conn.execute(text(
            "SELECT id, title, description, tag, project, status, solution FROM issue ORDER BY id"
        )).mappings().all()
    return [dict(r) for r in rows]
