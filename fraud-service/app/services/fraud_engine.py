from datetime import datetime, timedelta
from app.config import settings
from app.database import get_db
from app.models.fraud_assessment import FraudAssessment
import logging

logger = logging.getLogger(__name__)

# Hardcoded blocklist for demo purposes
BLOCKED_ACCOUNTS: set[str] = set()


async def evaluate(event: dict) -> FraudAssessment:
    """
    Evaluates a transaction event against all active fraud rules.
    Returns a FraudAssessment with a decision (APPROVED | REJECTED).

    Idempotency: if an assessment already exists for this transaction_id,
    return the existing result without re-evaluating.
    """
    db = get_db()
    transaction_id = str(event["transactionId"])

    # Idempotency check — avoid re-evaluating if we already assessed this transaction
    existing = await db.fraud_assessments.find_one({"transaction_id": transaction_id})
    if existing:
        logger.info("Idempotency hit: returning existing assessment for tx=%s", transaction_id)
        existing["_id"] = str(existing["_id"])
        return FraudAssessment(**existing)

    risk_score = 0
    reasons: list[str] = []

    # Rule 1: Amount threshold
    amount = float(event.get("amount", 0))
    rules = await db.fraud_rules.find({"enabled": True}).to_list(length=100)
    amount_rule = next((r for r in rules if r["name"] == "AMOUNT_THRESHOLD"), None)
    threshold = amount_rule["threshold_value"] if amount_rule else settings.amount_threshold

    if amount > threshold:
        contribution = amount_rule["risk_score_contribution"] if amount_rule else 50
        risk_score += contribution
        reasons.append("AMOUNT_THRESHOLD_EXCEEDED")
        logger.info("Rule AMOUNT_THRESHOLD triggered: amount=%.2f > threshold=%.2f", amount, threshold)

    # Rule 2: Velocity check — too many transactions from the same account in the window
    from_account_id = str(event.get("fromAccountId", ""))
    window_start = datetime.utcnow() - timedelta(minutes=settings.velocity_window_minutes)
    recent_count = await db.fraud_assessments.count_documents({
        "from_account_id": from_account_id,
        "evaluated_at": {"$gte": window_start},
        "decision": {"$ne": "REJECTED"},
    })
    velocity_rule = next((r for r in rules if r["name"] == "VELOCITY_CHECK"), None)
    max_tx = velocity_rule["threshold_value"] if velocity_rule else settings.velocity_max_transactions

    if recent_count >= int(max_tx):
        contribution = velocity_rule["risk_score_contribution"] if velocity_rule else 40
        risk_score += contribution
        reasons.append("VELOCITY_CHECK_EXCEEDED")
        logger.info("Rule VELOCITY_CHECK triggered: %d recent transactions", recent_count)

    # Rule 3: Blocked account
    to_account_id = str(event.get("toAccountId", ""))
    if from_account_id in BLOCKED_ACCOUNTS or to_account_id in BLOCKED_ACCOUNTS:
        risk_score += 100
        reasons.append("BLOCKED_ACCOUNT")
        logger.info("Rule BLOCKED_ACCOUNT triggered for account %s", from_account_id)

    decision = "REJECTED" if risk_score >= settings.risk_score_threshold else "APPROVED"

    assessment = FraudAssessment(
        transaction_id=transaction_id,
        from_account_id=from_account_id,
        to_account_id=to_account_id,
        amount=amount,
        currency=event.get("currency", "USD"),
        risk_score=risk_score,
        decision=decision,
        reasons=reasons,
    )

    result = await db.fraud_assessments.insert_one(assessment.model_dump(exclude={"id"}))
    assessment.id = str(result.inserted_id)

    logger.info("Assessment complete: tx=%s score=%d decision=%s", transaction_id, risk_score, decision)
    return assessment
