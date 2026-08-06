import pytest
from unittest.mock import AsyncMock, MagicMock
from datetime import datetime


@pytest.fixture
def mock_db(monkeypatch):
    """Patch get_db() to return a mock async MongoDB database."""
    db = MagicMock()

    # fraud_rules collection — return default rules
    rules_cursor = MagicMock()
    rules_cursor.to_list = AsyncMock(return_value=[
        {"name": "AMOUNT_THRESHOLD", "enabled": True,
         "risk_score_contribution": 50, "threshold_value": 10000.0},
        {"name": "VELOCITY_CHECK", "enabled": True,
         "risk_score_contribution": 40, "threshold_value": 5.0},
    ])
    db.fraud_rules.find.return_value = rules_cursor

    # fraud_assessments — no existing assessment by default
    db.fraud_assessments.find_one = AsyncMock(return_value=None)
    db.fraud_assessments.count_documents = AsyncMock(return_value=0)
    insert_result = MagicMock()
    insert_result.inserted_id = "mock-oid-123"
    db.fraud_assessments.insert_one = AsyncMock(return_value=insert_result)

    monkeypatch.setattr("app.services.fraud_engine.get_db", lambda: db)
    return db


@pytest.fixture
def existing_assessment(mock_db):
    """Pre-seed an existing assessment for idempotency tests."""
    mock_db.fraud_assessments.find_one = AsyncMock(return_value={
        "_id": "existing-oid",
        "transaction_id": "tx-existing-001",
        "from_account_id": "acc-001",
        "to_account_id": "acc-002",
        "amount": 500.0,
        "currency": "USD",
        "risk_score": 0,
        "decision": "APPROVED",
        "reasons": [],
        "evaluated_at": datetime.utcnow(),
    })
    return mock_db
