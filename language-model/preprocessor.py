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

# automatically loads API key from GEMINI_API_KEY environment variable
client = genai.Client()
Prompt = """Generate one question that can be answered completely and directly using only the following text. 
The question should target the main idea or a key fact in the text. Do not ask about information that is not explicitly stated. 
Do not add assumptions or require outside knowledge."""

# out = open(Path("textbooks") / "dataset.jsonl", "w", encoding="utf-8")
out = open(Path("dataset.jsonl"), "w", encoding="utf-8")
for item in Path("textbooks").iterdir():
    if not item.is_file() or item.stat().st_size == 0 or item.name.startswith("."):
        continue

    doc = None
    try:
        doc = pymupdf.open(item)

        if not doc.is_pdf:
            doc.close()
            continue

        header = ""
        paragraph = ""
        for page_num, page in enumerate(doc):
            if page_num < 107:
                continue
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
                        if (span["size"] == 20
                            or span["size"] == 18
                            or (span["size"] == 9.5 and (span["flags"] &16) and not (span["flags"] & 2))        # 9.5, bold, not italic
                            or (span["size"] == 12 and not (span["flags"] & 16) and not (span["flags"] & 2))    # 12, not bold, not italic
                        ):
                            # make sure that header is assigned to text that's font size: 20/18 or bold 
                            if header and paragraph and len(paragraph) >= 165 and len(paragraph) <= 3000:
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
                        if span["size"] == 9.5:
                            paragraph += span["text"] + " "

        if header and paragraph and len(paragraph) >= 165 and len(paragraph) <= 3000:
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