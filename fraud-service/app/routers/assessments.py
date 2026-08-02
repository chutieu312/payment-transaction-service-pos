from fastapi import APIRouter, HTTPException
from app.database import get_db

router = APIRouter(prefix="/fraud/assessments", tags=["Fraud Assessments"])


@router.get("/{transaction_id}")
async def get_assessment(transaction_id: str):
    db = get_db()
    doc = await db.fraud_assessments.find_one({"transaction_id": transaction_id})
    if not doc:
        raise HTTPException(status_code=404, detail="Assessment not found")
    doc["_id"] = str(doc["_id"])
    return doc
