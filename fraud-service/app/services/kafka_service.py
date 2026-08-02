import asyncio
import json
import logging
from datetime import datetime

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from app.config import settings
from app.services import fraud_engine

logger = logging.getLogger(__name__)

_producer: AIOKafkaProducer | None = None


async def get_producer() -> AIOKafkaProducer:
    global _producer
    if _producer is None:
        _producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v, default=str).encode("utf-8"),
        )
        await _producer.start()
    return _producer


async def publish_fraud_assessment(assessment, correlation_id: str):
    producer = await get_producer()
    payload = {
        "transactionId": assessment.transaction_id,
        "correlationId": correlation_id,
        "decision": assessment.decision,
        "riskScore": assessment.risk_score,
        "reasons": assessment.reasons,
        "timestamp": datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S.%f") + "Z",
    }
    await producer.send_and_wait(
        "fraud.assessment",
        value=payload,
        key=assessment.transaction_id.encode("utf-8"),
    )
    logger.info("Published fraud.assessment for tx=%s decision=%s",
                assessment.transaction_id, assessment.decision)


async def start_consumer():
    """Long-running Kafka consumer loop. Started as a background task on app startup."""
    consumer = AIOKafkaConsumer(
        "transaction.initiated",
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_consumer_group,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset="earliest",
        enable_auto_commit=True,
    )
    await consumer.start()
    logger.info("Fraud service Kafka consumer started, listening on transaction.initiated")
    try:
        async for msg in consumer:
            event = msg.value
            logger.info("Consumed transaction.initiated: txId=%s", event.get("transactionId"))
            try:
                assessment = await fraud_engine.evaluate(event)
                correlation_id = event.get("correlationId", "")
                await publish_fraud_assessment(assessment, correlation_id)
            except Exception as e:
                logger.error("Error processing transaction event: %s", e, exc_info=True)
    finally:
        await consumer.stop()
        logger.info("Kafka consumer stopped")
