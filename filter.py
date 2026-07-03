import json

bad = []
good = []

with open("./TrainingSet/dataset.jsonl", "r", encoding="utf-8") as f:
    for i, line in enumerate(f, 1):
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
            msgs = obj.get("messages", [])
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
                good.append(line)
        except json.JSONDecodeError as e:
            bad.append({
                "line": i,
                "error": str(e),
                "text": line[:200]  # preview only
            })

with open("./TrainingSet/dataset.jsonl", "w", encoding="UTF-8") as f:
    for line in good:
        f.write(line + "\n")

print(f"Invalid lines: {len(bad)}")

for b in bad[:100]:
    print(b["line"], b["error"])