# python imports
import pymupdf
from pathlib import Path
import re

minCharCount = 100
folderIn = "Reports"
folderOut = "Raw"

def main():
    """ Rules:
    Input/Output text must not overlap
    1. Select sentence with conclusion (output)
    2. Select rest of preceeding excerpt, preceeding, as input (3-5 sentences)
    3. Turn into sequentially-named file, in/out separated by \n
    
    """ 
    
    # run parser
    parser()
    print("Done")



""" Distil text files into useable input and output blocks
    1. Split text files into blocks (sections between newlines) of size > X characters
    2. Determine if block should be classified input or output based on 
        matched certain keywords. Drop block if does not contain keywords count > Y
    3. Match adjacent/consecutive input and output blocks to create training pairs

"""

def parser():
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

                cleaned = []
                for block in blocks:
                    if block[6] != 0:
                        continue
                    paragraph = " ".join(block[4].splitlines())
                    paragraph = " ".join(paragraph.split())
                    cleaned.append(paragraph)

                merged = []
                buffer = ""
                for paragraph in cleaned:
                    if buffer:
                        buffer = buffer + " " + paragraph
                    else:
                        buffer = paragraph
                    if buffer.endswith((".", "?", "!")):
                        if len(buffer) >= minCharCount:
                            merged.append(buffer)
                        buffer = ""
                if buffer:
                    merged.append(buffer)

                out.write("\n".join(merged) + "\n\n")

        print(outFile + " generated for " + item.name)
        x += 1


# gotta be at bottom, py runs top to bottom
if __name__ == "__main__":
    main()