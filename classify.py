from pathlib import Path
import re

inputDirectory = Path("Raw")
outputDirectory = Path("TrainingDirectry")

promptKeys = ["construction", "vacancy", "rent", "occupiers", "families"]
responseKeys = ["real estate", "investment activity" "leasing activity"]

def main():
    classify()


def classify():

    # cycle through /Raw
    for item in inputDirectory.iterdir():
        if not item.is_file() or item.stat().st_size == 0:
            continue

        # open text file (only match prompt-response by file) and open text file (to write out formatted jsonl)
        with open(item, "r", encoding="UTF-8") as raw: # and with open("outputDirectoy / outFile, "w", encoding="utf-8") as out:
            # split chunks of text into an array by \n, ignoring whitespace
            chunks = [c for c in re.split(r'\n{1,3}', raw.read()) if c.strip()]

            # if classification is same as before (i.e. two prompts in a row) drop chunk
            for chunk in chunks:

                promptPattern = "|".join(re.escape(x) for x in promptKeys)
                responsePattern = "|".join(re.escape(x) for x in responseKeys)

                promptCount = len(re.findall(promptPattern, chunk))
                responseCount = len(re.findall(responsePattern, chunk))

                # condition 1: prompt and response keywords
                if (promptCount > 1 and responseCount > 1):
                    print("Split the sentence")

                # condition 2: response keywords
                elif (promptCount > 1):
                    print("You got a prompt")

                # condition 3: prompt & response keywords
                elif (responseCount > 1):
                    print("You got a neither")

                else:
                    print("You got neither")
                # sentences = re.split(r'(?<=[.!?])\s+', chunk)


        # if match to prompt
        # then write to prompt

        # elif match to respnose
        # then write to prompt


        # close text file


""" 
Format into the form: 
{"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}
"""
# def format(prompt, response):

if __name__ == "__main__":
    main()