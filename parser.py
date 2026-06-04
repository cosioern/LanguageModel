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
    print("Done")

# turn pdf into text file, naming scheme: fileX.txt
def parsePDF(folderIn, folderOut):

    # determine the number of files to convert to text
    folder = Path(folderIn)
    outDir = Path(folderOut)

    x = 1
    for item in folder.iterdir():
        if item.is_file():

            # skip empty files
            if item.stat().st_size == 0:
                continue

            doc = pymupdf.open(item)

            # skip non pdfs
            if (not doc.is_pdf):
                continue

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




# gotta be at bottom, py runs top to bottom
if __name__ == "__main__":
    main()
