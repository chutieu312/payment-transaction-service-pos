from app.database import get_db
from app.models.fraud_rule import FraudRule

DEFAULT_RULES = [
    FraudRule(
        name="AMOUNT_THRESHOLD",
        description="Flag transactions above a dollar threshold",
        enabled=True,
        risk_score_contribution=50,
        threshold_value=10000.0,
    ),
    FraudRule(
        name="VELOCITY_CHECK",
        description="Flag accounts with too many transactions in a short window",
        enabled=True,
        risk_score_contribution=40,
        threshold_value=5.0,
    ),
    FraudRule(
        name="BLOCKED_ACCOUNT",
        description="Auto-reject transactions involving blocked accounts",
        enabled=True,
        risk_score_contribution=100,
        threshold_value=None,
    ),
]


async def seed_default_rules():
    db = get_db()
    for rule in DEFAULT_RULES:
        existing = await db.fraud_rules.find_one({"name": rule.name})
        if not existing:
            await db.fraud_rules.insert_one(rule.model_dump(exclude={"id"}))
