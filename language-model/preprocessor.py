import pymupdf
from pathlib import Path
from google import genai
from google.genai.errors import APIError, ClientError
import json
import time
import re

# clean up text by removing hyphenation, newlines, and extra spaces
def clean_text(text) -> str:
    text = re.sub(r"(\w)-\s+(\w)", r"\1\2", text)
    text = text.replace("\n", " ")
    text = re.sub(r"([a-z])(\d)", r"\1 \2", text)
    text = re.sub(r"(\d)([a-zA-Z])", r"\1 \2", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()

def font_size_is(span, size, tolerance=0.1):
    return abs(span["size"] - size) < tolerance

def is_noise(text):
    text = text.strip()

    if len(re.findall(r"[A-Za-z]", text)) < 10:
        return True

    patterns = [
        r"^Figure \d:"
        r"^Intelligent Investment",
        r"^CBRE RESEARCH",
        r"^Source:",
        r"^Figure \d+",
        r"^\d+\s+CBRE RESEARCH",
        r"^(Vacancy Rate|Availability Rate|Renewal Share)\b",
        r"^New Leases|Renewals|Renewals %",
        r"^\d{2}\s+(Economy|Office|Industrial|Retail|Healthcare|Capital Markets)",
    ]

    return any(re.search(p, text, re.I) for p in patterns)

def split_paragraph(text, max_len=3000):
    sentences = re.split(r'(?<=[.!?])\s+', text)
    chunks = []
    current = ""
    for sentence in sentences:
        if len(current) + len(sentence) > max_len:
            if current:
                chunks.append(current.strip())
            current = sentence
        else:
            current += " " + sentence
    if current:
        chunks.append(current.strip())
    return chunks

# automatically loads API key from GEMINI_API_KEY environment variable
client = genai.Client()
Prompt = """Generate one question that can be answered completely and directly using only the following text. 
The question should target the main idea or a key fact in the text. Do not ask about information that is not explicitly stated. 
Do not add assumptions or require outside knowledge."""

# out = open(Path("dataset.jsonl"), "w", encoding="utf-8")
out = open(Path("TrainingSet/cbre2025.jsonl"), "w", encoding="utf-8")
for item in Path("./Reports/current").iterdir():
    if not item.is_file() or item.stat().st_size == 0 or item.name.startswith("."):
        continue

    doc = None
    try:
        doc = pymupdf.open(item)

        if not doc.is_pdf:
            doc.close()
            continue

        header = item.name
        paragraph = ""
        for page_num, page in enumerate(doc):
            # if page_num < 20:
            #     continue
            try: 
                page_dict = page.get_text("dict")
            except Exception as e:
                print(f"Failed to extract text from page {page_num} from {item.name}: {str(e)}")
                continue

            for block in page_dict["blocks"]:
                if block["type"] != 0:
                    continue

                for line in block["lines"]:
                    for span in line["spans"]:
                        font_size = span["size"]
                        flags = span["flags"]
                        # print("Font size: " + f"{font_size}" + " and text: " + span["text"] + " and isBold: " + f"{flags & 16}" + " and isItalic: " + f"{flags & 2}")
                        # if (span["size"] == 20
                        #     or span["size"] == 18
                        #     or (span["size"] == 9.5 and (span["flags"] &16) and not (span["flags"] & 2))        # 9.5, bold, not italic
                        #     or (span["size"] == 12 and not (span["flags"] & 16) and not (span["flags"] & 2))    # 12, not bold, not italic
                        # ):
                        # if (font_size_is(span, 54)
                        #     or font_size_is(span, 34)
                        #     or font_size_is(span, 11) and span["flags"] & 16
                        #     or font_size_is(span, 16)
                        # ):
                        # split off a prompt-response pair
                        if (font_size_is(span, 36) or font_size_is(span, 14) or font_size_is(span, 19)):

                            if header and paragraph and len(paragraph) >= 165: # and len(paragraph) <= 3000:
                                chunks = split_paragraph(paragraph) if len(paragraph) > 3000 else [paragraph]
                                for chunk in chunks:
                                    if len(chunk) >= 165:
                                        
                                        # print("\nHEADER:", header)
                                        # print("PARAGRAPH", clean_text(paragraph))
                                        # print("-" * 80)

                                        while True:
                                            try:
                                                paragraph = clean_text(paragraph)
                                                interaction = client.interactions.create(
                                                    # model="gemini-3.6-flash",
                                                    model="gemini-3.1-flash-lite",
                                                    input=f"{Prompt}, Header: {header}, Text: {paragraph}",
                                                    generation_config={
                                                        "temperature": 0.3
                                                    }
                                                )
                                                if interaction.status == "completed" and interaction.output_text:
                                                    out.write(json.dumps({
                                                        "messages": [
                                                            {"role": "user", "content": interaction.output_text},
                                                            {"role": "assistant", "content": paragraph}
                                                        ]
                                                    }) + "\n")
                                                    time.sleep(5)
                                                    break
                                                    # print(f"Prompt: {interaction.output_text}\nResponse: {paragraph}\n\n")
                                                else:
                                                    print("Failure")
                                                    time.sleep(5)

                                            except ClientError as e:
                                                if e.code == "429":
                                                    print(f"Rate Limit Exceeded. Waiting 30 seconds")
                                                    time.sleep(30)
                                                else:
                                                    print(f"Fix your code goofball: {e.code} - {e.message}")
                                            # except ServiceError as e:
                                            #     print(f"Google's fault: {e.code} - {e.message}")
                                            except APIError as e:
                                                print(f"Something went wrong: {e.code} - {e.message}")
                                                break

                            # set new header and reset paragraph
                            header = span["text"]
                            paragraph = ""
                        # if span["size"] == 9.5:
                        # if font_size_is(span, 10):
                        # if 9.5 <= span["size"] <= 10.5:
                        if (font_size_is(span, 11) or font_size_is(span, 12)):
                            if span["text"].strip() and not is_noise(span["text"]):
                                paragraph += span["text"] + " "

        if header and paragraph and len(paragraph) >= 165: # and len(paragraph) <= 3000:
            chunks = split_paragraph(paragraph) if (len(paragraph) > 3000) else [paragraph]
            for chunk in chunks:
                if len(chunk) >= 165:

                    # print("\nHEADER:", header)
                    # print("PARAGRAPH", clean_text(paragraph))
                    # print("-" * 80)

                    while True:
                        try:
                            paragraph = clean_text(paragraph)
                            interaction = client.interactions.create(
                                # model="gemini-3.6-flash",
                                model="gemini-3.1-flash-lite",
                                input=f"{Prompt}, Header: {header}, Text: {paragraph}",
                                generation_config={
                                    "temperature": 0.3
                                }
                            )
                            if interaction.status == "completed" and interaction.output_text:
                                # print(f"Prompt: {interaction.output_text}\nResponse: {paragraph}\n\n")
                                out.write(json.dumps({
                                    "messages": [
                                        {"role": "user", "content": interaction.output_text},
                                        {"role": "assistant", "content": paragraph}
                                    ]
                                }) + "\n")
                                time.sleep(5)
                                break
                            else:
                                print("Failure")
                                time.sleep(5)
                        except ClientError as e:
                            if e.code == "429":
                                print(f"Rate Limit Exceeded. Waiting 30 seconds")
                                time.sleep(30)
                            else:
                                print(f"Fix your code goofball: {e.code} - {e.message}")
                                break
                        # except ServiceError as e:
                        #     print(f"Google's fault: {e.code} - {e.message}")
                        except APIError as e:
                            print(f"Something went wrong: {e.code} - {e.message}")
                            break

    except Exception as e:
        print(f"Error: {str(e)}")
        # continue
    finally:
        if doc:
            doc.close()

out.close() 