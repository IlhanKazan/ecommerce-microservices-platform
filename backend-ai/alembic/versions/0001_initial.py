"""initial ai_db schema — chat + analytics

Revision ID: 0001_initial
Revises:
Create Date: 2026-06-17
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = "0001_initial"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "chat_sessions",
        sa.Column(
            "id",
            postgresql.UUID(as_uuid=True),
            primary_key=True,
            server_default=sa.text("gen_random_uuid()"),
        ),
        sa.Column("user_keycloak_id", sa.String(length=36), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.Column(
            "last_activity_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )
    op.create_index(
        "idx_chat_sessions_user", "chat_sessions", ["user_keycloak_id"]
    )

    op.create_table(
        "chat_messages",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("session_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("role", sa.String(length=20), nullable=False),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.ForeignKeyConstraint(
            ["session_id"], ["chat_sessions.id"], ondelete="CASCADE"
        ),
    )
    op.create_index(
        "idx_chat_msgs_session",
        "chat_messages",
        ["session_id", "created_at"],
    )

    op.create_table(
        "product_view_events",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("user_keycloak_id", sa.String(length=36), nullable=True),
        sa.Column("product_id", sa.BigInteger(), nullable=False),
        sa.Column("tenant_id", sa.BigInteger(), nullable=False),
        sa.Column(
            "viewed_at",
            sa.DateTime(),
            nullable=False,
            server_default=sa.text("now()"),
        ),
    )
    op.create_index(
        "idx_views_user",
        "product_view_events",
        ["user_keycloak_id", "viewed_at"],
    )
    op.create_index(
        "idx_views_product",
        "product_view_events",
        ["product_id"],
    )

    op.create_table(
        "ai_summary_jobs",
        sa.Column(
            "product_id", sa.BigInteger(), primary_key=True, autoincrement=False
        ),
        sa.Column(
            "review_count_at_last_run",
            sa.Integer(),
            nullable=False,
            server_default="0",
        ),
        sa.Column("last_run_at", sa.DateTime(), nullable=True),
        sa.Column(
            "status",
            sa.String(length=20),
            nullable=False,
            server_default="PENDING",
        ),
        sa.Column("error_message", sa.Text(), nullable=True),
    )


def downgrade() -> None:
    op.drop_table("ai_summary_jobs")
    op.drop_index("idx_views_product", table_name="product_view_events")
    op.drop_index("idx_views_user", table_name="product_view_events")
    op.drop_table("product_view_events")
    op.drop_index("idx_chat_msgs_session", table_name="chat_messages")
    op.drop_table("chat_messages")
    op.drop_index("idx_chat_sessions_user", table_name="chat_sessions")
    op.drop_table("chat_sessions")
