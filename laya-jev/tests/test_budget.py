"""Exercise the actual upstream sequence builder without downloading a checkpoint."""
from types import SimpleNamespace

import pytest
from laya.agent import Agent
from laya.common import build_sequence

from app import InputBudgetError, LayaEngine, Settings


class Tokenizer:
    mask_token = "[MASK]"
    mask_token_id = 3
    cls_token_id = 1
    sep_token_id = 2

    def __call__(self, text, **kwargs):
        return {"input_ids": [ord(char) + 10 for char in text]}


def engine(settings=Settings()):
    result = LayaEngine(settings)
    result.agent = SimpleNamespace(tok=Tokenizer(), _to_internal=Agent._to_internal)
    return result


def test_dynamic_budget_preserves_long_instructions_and_latest_state_in_mixed_batch():
    model = engine()
    state = {"history": ["old context " * 10], "question": "latest question stays here"}
    questions = {
        "emotion": {"type": "choice", "instructions": "long example " * 120,
                    "criteria": {"happy": "short", "sad": {"description": "structured"}}},
        "urgent": {"type": "noul", "instructions": {"rules": ["urgent?", "[MASK]"]}},
        "score": {"type": "score", "instructions": ["rate", "quality"],
                  "criteria": ["low", {"meaning": "high"}]},
    }
    head = model.prepare(state, questions)
    assert head > 256  # The upstream default would silently lose examples.
    for question in questions.values():
        internal = model.agent._to_internal(question)
        actual = build_sequence(model.agent.tok, state, internal, 8192, head)
        unlimited = build_sequence(model.agent.tok, state, internal, 100000, 100000)
        assert actual == unlimited


def test_exact_sequence_boundary_and_one_token_overflow():
    model = engine(Settings(max_tokens=100))
    question = {"type": "choice", "instructions": "pick", "criteria": {"one": None}}
    internal = model.agent._to_internal(question)
    empty, _ = build_sequence(model.agent.tok, "", internal, 10000, 10000)
    state = "x" * (100 - len(empty))
    head = model.prepare(state, {"q": question})
    assert len(build_sequence(model.agent.tok, state, internal, 100, head)[0]) == 100
    with pytest.raises(InputBudgetError, match="LAYA_MAX_TOKENS"):
        model.prepare(state + "x", {"q": question})


def test_batch_limit_counts_padding_not_only_actual_tokens():
    model = engine(Settings(max_batch_tokens=600))
    short = {"type": "choice", "instructions": "pick", "criteria": {"one": None}}
    long = {**short, "instructions": "x" * 350}
    model.prepare("state", {"long": long})
    with pytest.raises(InputBudgetError, match="LAYA_MAX_BATCH_TOKENS"):
        model.prepare("state", {"short": short, "long": long})


@pytest.mark.parametrize("count", [1, 255])
def test_choice_boundary_keeps_every_option_marker(count):
    model = engine()
    question = {"type": "choice", "instructions": "pick", "criteria": {
        str(index): None for index in range(count)}}
    head = model.prepare("state", {"q": question})
    _, markers = build_sequence(model.agent.tok, "state", model.agent._to_internal(question),
                                8192, head)
    assert len(markers) == count
