# python imports
import pymupdf
from pathlib import Path 


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
    folderIn = "Reports"
    folderOut = "Raw"
    parsePDF(folderIn, folderOut)
    distilText(folderOut)
    print("Done")

# turn pdf into text file, naming scheme: fileX.txt
def parsePDF(folderIn, folderOut):

    # determine the number of files to convert to text
    inDir = Path(folderIn)
    outDir = Path(folderOut)

    x = 1
    for item in inDir.iterdir():
        if item.is_file():

            # skip empty files
            if item.stat().st_size == 0:
                continue

            doc = pymupdf.open(item)

            # skip non pdfs
            if (not doc.is_pdf):
                continue

            # print TOC
            toc = doc.get_toc()
            
            if (not toc):
                print("Must Infer TOC")

            # generate rawX.txt for each report (and each page)
            outFile = "raw" + str(x) + ".txt"
            out = open(outDir / outFile, "wb")
            for page in doc:
                text = page.get_text().encode("utf8")
                out.write(text)
                out.write(bytes((12,)))
            out.close()
            print(outFile + " generated for " + item.name)
            x += 1

""" Distil text files into useable input and output blocks
    1. Split text files into blocks (sections between newlines) of size > X characters
    2. Determine if block should be classified input or output based on 
        matched certain keywords. Drop block if does not contain keywords count > Y
    3. Match adjacent/consecutive input and output blocks to create training pairs

"""
def distilText(folderOut):

    minCharCount = 50
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
        blocks = text.split("\n")
        filtered = [b for b in blocks if len(b.strip()) >= minCharCount]
        print("\n".join(filtered))

            # # helper function removing small sections of text from raw file
            # def dropSmallSections(item):



# gotta be at bottom, py runs top to bottom
if __name__ == "__main__":
    main()
