# python imports
import pymupdf
from pathlib import Path
import re

minCharCount = 50
folderIn = "Reports"
folderOut = "Raw"

def main():

    # extract certain data based on keywords in/out

    # spit out into text file with names such as inX.text (where Xth number denotes
    # how many files have been ingested / processed)

    """ Rules:
    Input/Output text must not overlap
    1. Select sentence with conclusion (output)
    2. Select rest of preceeding excerpt, preceeding, as input (3-5 sentences)
    3. Turn into sequentially-named file, in/out separated by \n
    
    """ 
    #
    parsePDF(folderIn, folderOut)
    distilText(folderOut)
    print("Done")

def parsePDF(folderIn, folderOut):
    inDir = Path(folderIn)
    outDir = Path(folderOut)

    x = 1
    for item in inDir.iterdir():
        if not item.is_file() or item.stat().st_size == 0:
            continue

        doc = pymupdf.open(item)
        if not doc.is_pdf:
            continue

        outFile = "raw" + str(x) + ".txt"
        with open(outDir / outFile, "w", encoding="utf-8") as out:
            for page in doc:
                blocks = page.get_text("blocks")
                
                # Collect and clean text blocks first
                cleaned = []
                for block in blocks:
                    if block[6] != 0:
                        continue
                    paragraph = " ".join(block[4].splitlines())
                    paragraph = " ".join(paragraph.split())
                    if len(paragraph) < minCharCount:
                        continue
                    cleaned.append(paragraph)

                # Merge blocks that don't end with terminal punctuation
                merged = []
                buffer = ""
                for paragraph in cleaned:
                    if buffer:
                        buffer = buffer + " " + paragraph
                    else:
                        buffer = paragraph
                    
                    if buffer.endswith((".", "?", "!")):
                        merged.append(buffer)
                        buffer = ""
                
                if buffer:  # flush any remaining text
                    merged.append(buffer)

                for paragraph in merged:
                    out.write(paragraph + "\n\n")

""" Distil text files into useable input and output blocks
    1. Split text files into blocks (sections between newlines) of size > X characters
    2. Determine if block should be classified input or output based on 
        matched certain keywords. Drop block if does not contain keywords count > Y
    3. Match adjacent/consecutive input and output blocks to create training pairs

"""
def distilText(folderOut):

    # minCharCount = 50
    # keywords for input blocks
    inputWords = ["construction", "vacancy", "rent", "occupiers", "families"]
    # keywords for output blocks
    outputWords = ["real estate", "investment activity" "leasing activity"]

    # words = page.get_text("words")
    # word_count = len(words)

    # for each raw text file
    folder = Path(folderOut)
    for item in folder.iterdir():
        print(item.name)
        with open(item, "r", encoding="utf-8") as f:
            text = f.read()

    # normalize only PDF artifacts
    text = text.replace("\r\n", "\n")
    text = text.replace("\x0c", "\n")

    # rebuild paragraph-like blocks
    lines = text.split("\n")

    rebuilt = []
    buffer = []

    for line in lines:
        line = line.strip()

        if not line:
            if buffer:
                rebuilt.append(" ".join(buffer))
                buffer = []
        else:
            buffer.append(line)

    if buffer:
        rebuilt.append(" ".join(buffer))

    filtered = [
        b.strip()
        for b in rebuilt
        if len(b.strip()) >= minCharCount
    ]

    print("\n\n".join(filtered))

# gotta be at bottom, py runs top to bottom
if __name__ == "__main__":
    main()
