import json
import re

bad = []
good = []
removed_axis_junk = []
scrub = [
    r"Real Estate Market Outlook",
    r"Chapter\s*\d+",
    r"UNITED STATES OUTLOOK \d{4}",
    "3 NOTE FROM OUR CHIEF ECONOMIST A Year of Extraordinary Challenges",
    "C R E O U T L O O K 2 0 2 5 Resilience & Recovery:",
    "U.S. National Retail Forecast",
    "U.S. NATIONAL OFFICE FORECAST",
    "U.S. NATIONAL LIFE SCIENCES FORECAST",
    "U.S. NATIONAL HOSPITALITY FORECAST",
    "CBRE RESEARCH U.S.",
    # r"Table\s\d",
    "Trends to Watch",
    r"Outlook\s\d{2}",
    # "Source:",
    r"\d+\sof\s\d+",
    r"(?:Q[1-4]\s\d{4}\s*){4,}",
    r"(?:[EF]?\s*Q[1-4]\s\d{4}\s*){4,}[EF]?,?",
    r"Table\s*\d+.*?(Source:|Note:)",
    "CBRE RESEARCH UNITED STATES",
    "Costar",
    "Cushman & Wakefield Research",
    "Source: U.S. Bureau of Labor Statistics",
    "U.S. Bureau of Labor Statistics",
    "Sources: U.S. Bureau of Economic Analysis",
    # add more literal phrases or regex patters
]

axis_junk_re = re.compile(
    r"(?:[\(\$]?-?\d{1,4}(?:\.\d+)?[FfEeAa]?%?\)?[\s,]*){6,}"
)

scrub_re = re.compile("|".join(scrub), re.IGNORECASE)

# remove noisy phrases
def scrub_content(text):
    cleaned = scrub_re.sub("", text)

    for m in axis_junk_re.finditer(cleaned):
        removed_axis_junk.append(m.group())
    cleaned = axis_junk_re.sub("", cleaned)

    # collapse leftover double spaces created by the removal
    cleaned = re.sub(r"\s{2,}", " ", cleaned).strip()
    return cleaned

# remove invalid JSON characters and lines with empty response content
with open("./TrainingSet/dataset.jsonl", "r", encoding="utf-8") as f:
    for i, line in enumerate(f, 1):
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
            msgs = obj.get("messages", [])

            for m in msgs:
                if isinstance(m.get("content"), str):
                    m["content"] = scrub_content(m["content"])

            empty_response = any(
                m.get("role") == "assistant" and not m.get("content", "").strip()
                for m in msgs
            )
            if empty_response:
                bad.append({
                    "line":i,
                    "error": "Empty Assistant Response",
                    "text": line[:200]
                })
            else:
                good.append(json.dumps(obj, ensure_ascii=False))
        except json.JSONDecodeError as e:
            bad.append({
                "line": i,
                "error": str(e),
                "text": line[:200]  # preview only
            })

# write correct lines back to file
with open("./TrainingSet/dataset.jsonl", "w", encoding="UTF-8") as f:
    for line in good:
        f.write(line + "\n")

# print bad lines to terminal for review
print(f"Invalid lines: {len(bad)}")
for b in bad[:100]:
    print(b["line"], b["error"])

print(f"\nTotal axis-junk spans removed: {len(removed_axis_junk)}")
for r in removed_axis_junk[:30]:
    print(repr(r))