from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    mongo_uri: str = "mongodb://localhost:27017"
    mongo_db: str = "fraud_db"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "fraud-service"
    risk_score_threshold: int = 70
    amount_threshold: float = 10000.0
    velocity_window_minutes: int = 60
    velocity_max_transactions: int = 5

    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
