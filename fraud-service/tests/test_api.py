import pytest
from httpx import AsyncClient, ASGITransport
from unittest.mock import AsyncMock, MagicMock
from main import app


@pytest.fixture
def mock_db_for_api(monkeypatch):
    db = MagicMock()
    db.fraud_rules.find.return_value = MagicMock(to_list=AsyncMock(return_value=[]))
    db.fraud_assessments.find_one = AsyncMock(return_value=None)
    insert_result = MagicMock()
    insert_result.inserted_id = "oid-1"
    db.fraud_assessments.insert_one = AsyncMock(return_value=insert_result)
    db.fraud_assessments.find.return_value = MagicMock(
        sort=MagicMock(return_value=MagicMock(
            limit=MagicMock(return_value=MagicMock(
                to_list=AsyncMock(return_value=[])
            ))
        ))
    )
    monkeypatch.setattr("app.routers.rules.get_db", lambda: db)
    monkeypatch.setattr("app.routers.assessments.get_db", lambda: db)
    monkeypatch.setattr("app.routers.alerts.get_db", lambda: db)
    return db


@pytest.mark.asyncio
async def test_health_check():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


@pytest.mark.asyncio
async def test_list_rules_empty(mock_db_for_api):
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.get("/fraud/rules/")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)


@pytest.mark.asyncio
async def test_get_assessment_not_found(mock_db_for_api):
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.get("/fraud/assessments/nonexistent-tx")
    assert resp.status_code == 404


@pytest.mark.asyncio
async def test_list_alerts_empty(mock_db_for_api):
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        resp = await client.get("/fraud/alerts/")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)
