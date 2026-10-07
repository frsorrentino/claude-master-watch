"""Test dello script delle misure del relay (piano prestazioni, Task 0). Uso: python3 -m pytest scripts/test_relay_measures.py"""
import importlib.util
import pathlib

spec = importlib.util.spec_from_file_location("relay_measures", pathlib.Path(__file__).with_name("relay-measures.py"))
rm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rm)

LOG = """2026-10-07T16:00:00 push: 7 sessioni, 0 eventi
2026-10-07T16:00:30 push: 7 sessioni, 1 eventi
2026-10-07T16:01:30 push: 7 sessioni, 0 eventi
2026-10-07T16:00:10 cmd a1: transcript master da web (locale) → ok {"entries": []}
2026-10-07T16:00:20 cmd a2: transcript master da Pixel 11 Pro XL → ok {"entries": []}
2026-10-07T16:00:25 cmd a3: projects da Pixel 11 Pro XL → ok {}
2026-10-07T16:30:00 cmd a4: prompt master da web (locale) → ok delivered
2026-10-07T17:00:00 cmd a5: prompt master da web (locale) → ok delivered
2026-10-06T16:10:00 push: 7 sessioni, 0 eventi
""".splitlines()


def test_pushes_and_intervals():
    d = rm.measure(LOG, "16:00", "17:00", "2026-10-07")
    assert d["push_per_hour"] == 3.0
    assert d["push_interval_s"]["median"] == 45.0
    assert d["push_interval_s"]["max"] == 60.0


def test_commands_per_device_without_a_session_too():
    d = rm.measure(LOG, "16:00", "17:00", "2026-10-07")["per_hour"]
    assert d["transcript da web (locale)"] == 1.0
    assert d["transcript da Pixel 11 Pro XL"] == 1.0
    assert d["projects da Pixel 11 Pro XL"] == 1.0
    assert d["prompt da web (locale)"] == 1.0   # 17:00 è fuori dalla finestra


def test_other_days_are_left_out():
    assert rm.measure(LOG, "16:00", "17:00", "2026-10-06")["push_per_hour"] == 1.0
