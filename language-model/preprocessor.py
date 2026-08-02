import pymupdf
from pathlib import Path
from openai import OpenAI

inDir = "textbooks"
outDir = "tec"

# automatically loads API key from OPENAI_API_KEY environment variable
client = OpenAI()
Prompt = "Generate one question that is answered completely by the following text, do not invent information."

# 1. extract a section
out = open(Path("textbooks") / "dataset.jsonl", "w", encoding="utf-8")
for item in Path("textbooks").iterdir():
    if not item.is_file() or item.stat().st_size == 0 or item.name.startswith("."):
        continue

    doc = pymupdf.open(item)
    if not doc.is_pdf:
        continue

    header = ""
    paragraph = ""
    for page_num, page in enumerate(doc):
        if page_num < 20:
            continue
        page_dict = page.get_text("dict")
        for block in page_dict["blocks"]:
            if block["type"] != 0:
                continue
            for line in block["lines"]:
                for span in line["spans"]:
                    # font_size = span["size"]
                    # print("Font size: " + f"{font_size}" + " and text: " + span["text"])
                    if span["size"] == 20 or span["size"] == 18:
                        if header and paragraph:
                            # send out request to OpenAI
                            response = client.chat.completions.create(
                                model="gpt-4o-mini",
                                messages=[
                                    {"role":"user", "content": f"{Prompt}, Header: {header}, Text: {paragraph}"}
                                ],
                                temperature=0.3,
                            )
                            # format OpenAI response to jsonl line and write to file
                            print(header + "\n" + paragraph + "\n\n")
                            pass
                        # set new header and reset paragraph
                        header = span["text"]
                        paragraph = ""
                    if span["size"] == 9.5:
                        paragraph += span["text"] + " "

    if header and paragraph:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role":"user", "content": f"{Prompt}, Header: {header}, Text: {paragraph}"}
            ],
            temperature=0.3,
        )

    doc.close()

out.close()

# 2. send to LLM for processing, with a fixed prompt

# 3. receive a question

# 4. format into pair

#5. 