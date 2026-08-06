from fastapi import APIRouter
from app.database import get_db
from app.models.fraud_rule import FraudRule

router = APIRouter(prefix="/fraud/rules", tags=["Fraud Rules"])


@router.get("/", response_model=list[dict])
async def list_rules():
    db = get_db()
    rules = await db.fraud_rules.find({}).to_list(length=100)
    for r in rules:
        r["_id"] = str(r["_id"])
    return rules


@router.post("/", status_code=201)
async def create_or_update_rule(rule: FraudRule):
    db = get_db()
    existing = await db.fraud_rules.find_one({"name": rule.name})
    if existing:
        await db.fraud_rules.update_one(
            {"name": rule.name},
            {"$set": rule.model_dump(exclude={"id"})},
        )
        return {"message": f"Rule '{rule.name}' updated"}
    await db.fraud_rules.insert_one(rule.model_dump(exclude={"id"}))
    return {"message": f"Rule '{rule.name}' created"}
