from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class FraudAssessment(BaseModel):
    id: Optional[str] = Field(None, alias="_id")
    transaction_id: str
    from_account_id: str
    to_account_id: str
    amount: float
    currency: str
    risk_score: int
    decision: str          # APPROVED | REJECTED
    reasons: list[str]
    evaluated_at: datetime = Field(default_factory=datetime.utcnow)

    model_config = {"populate_by_name": True}
