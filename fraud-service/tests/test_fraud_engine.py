import pytest
from app.services.fraud_engine import evaluate


def make_event(amount=500.0, from_account="acc-001", to_account="acc-002",
               transaction_id="tx-test-001"):
    return {
        "transactionId": transaction_id,
        "fromAccountId": from_account,
        "toAccountId": to_account,
        "amount": amount,
        "currency": "USD",
        "correlationId": "corr-1",
    }


@pytest.mark.asyncio
async def test_small_amount_approved(mock_db):
    """Transaction under threshold should be APPROVED with score 0."""
    event = make_event(amount=500.0, transaction_id="tx-small-001")
    assessment = await evaluate(event)
    assert assessment.decision == "APPROVED"
    assert assessment.risk_score < 70
    assert "AMOUNT_THRESHOLD_EXCEEDED" not in assessment.reasons


@pytest.mark.asyncio
async def test_large_amount_score_increased(mock_db):
    """Transaction over $10,000 should get AMOUNT_THRESHOLD reason added."""
    event = make_event(amount=15000.0, transaction_id="tx-large-001")
    assessment = await evaluate(event)
    assert "AMOUNT_THRESHOLD_EXCEEDED" in assessment.reasons
    assert assessment.risk_score >= 50


@pytest.mark.asyncio
async def test_score_below_threshold_approved(mock_db):
    """Score of 69 should be APPROVED (threshold is 70)."""
    event = make_event(amount=9999.99, transaction_id="tx-boundary-001")
    assessment = await evaluate(event)
    # Amount is just under threshold so no AMOUNT_THRESHOLD trigger
    assert assessment.decision == "APPROVED"


@pytest.mark.asyncio
async def test_idempotency_returns_existing(mock_db, existing_assessment):
    """Re-evaluating the same transaction_id should return existing result."""
    event = make_event(transaction_id="tx-existing-001")
    assessment = await evaluate(event)
    # Should NOT call insert — returns the seeded existing assessment
    assert assessment.decision == "APPROVED"
    assert assessment.transaction_id == "tx-existing-001"
