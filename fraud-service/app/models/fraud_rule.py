from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class FraudRule(BaseModel):
    id: Optional[str] = Field(None, alias="_id")
    name: str
    description: str
    enabled: bool = True
    risk_score_contribution: int
    threshold_value: Optional[float] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    model_config = {"populate_by_name": True}
