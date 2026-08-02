import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from app.routers import rules, assessments, alerts
from app.seed.default_rules import seed_default_rules
from app.services.kafka_service import start_consumer

logging.basicConfig(level=logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await seed_default_rules()
    consumer_task = asyncio.create_task(start_consumer())
    yield
    # Shutdown
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass


app = FastAPI(
    title="Fraud Detection Service",
    description="Consumes transaction events from Kafka, evaluates fraud rules, publishes assessment.",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(rules.router)
app.include_router(assessments.router)
app.include_router(alerts.router)


@app.get("/health", tags=["Health"])
async def health():
    return {"status": "ok", "service": "fraud-detection-service"}
