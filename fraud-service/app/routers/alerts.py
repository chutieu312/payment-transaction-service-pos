from fastapi import APIRouter
from app.database import get_db

router = APIRouter(prefix="/fraud/alerts", tags=["Fraud Alerts"])


@router.get("/")
async def list_alerts(limit: int = 50):
    db = get_db()
    docs = await db.fraud_assessments.find({"decision": "REJECTED"}) \
        .sort("evaluated_at", -1).limit(limit).to_list(length=limit)
    for d in docs:
        d["_id"] = str(d["_id"])
    return docs
