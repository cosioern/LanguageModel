from pathlib import Path
import re

inputDirectory = Path("Raw")
outputDirectory = Path("TrainingSet")
outFile = "train.jsonl"

""" Property Value = NOI / CR ~ if cap rate falls an NOI remains same, property values increase.
    Good for current holders (can sell higher). Current buyers are betting on future growth.
"""
# better to have fewer, more specifically relevant keywords + lower count barrier
promptKeys =    ["market conditions", "inflation", "supply and demand", "vacancy", "net absorption", 
                "market share"
                ]
responseKeys = ["real estate", "investment activity", "cap rates", "leasing activity", 
                "leasing volume", "depreciate", "we expect", "will likely", "expected to", 
                "fundamentals", "supply overhang", "will continue", "we remain", "is expected",
                "should continue", "forcasts"
                ]
discardKeys =   ["copyright", "all rights reserved", "disclaimer", "disclaims all liability",
                "waive all claims", "figure"
                ]


def main():
    classify()
    format("MJ's catch phrase?", "HEEEEE HEEEEEE`!")

""" 
Potential functionality to add: Classfy more distinctly (e.g. by market data, context, analyst commentary, investment outlook / prediction)
!!!Combine consecutive prompts or consecutive responses rather than dropping.!!!
"""
def classify():

    # patterns to classify chunks of text
    promptPattern = "|".join(re.escape(x) for x in promptKeys)
    responsePattern = "|".join(re.escape(x) for x in responseKeys)
    discardPattern = "|".join(re.escape(x) for x in discardKeys)

    # cycle through /Raw
    for item in inputDirectory.iterdir():
        if not item.is_file() or item.stat().st_size == 0:
            continue
        promptBuffer = ""
        responseBuffer = ""

        # open text file (only match prompt-response by file) and open text file (to write out formatted jsonl)
        with open(item, "r", encoding="UTF-8") as raw: # and with open("outputDirectoy / outFile, "w", encoding="utf-8") as out:
            # split chunks of text into an array by \n, ignoring whitespace
            chunks = [c for c in re.split(r'\n{1,3}', raw.read()) if c.strip()]

            # if classification is same as before (i.e. two prompts in a row) drop chunk
            for chunk in chunks:

                # discard copyright / contact info / section headers / chart data (noise)
                if (re.search(discardPattern, chunk, re.IGNORECASE)
                or re.search(r'[\w.-]+@[\w.-]+', chunk, re.IGNORECASE)
                or not (re.search(r'[.!?](\s|$)', chunk, re.IGNORECASE))):
                    continue

                promptCount = len(re.findall(promptPattern, chunk, re.IGNORECASE))
                responseCount = len(re.findall(responsePattern, chunk, re.IGNORECASE))

                # condition 1: discard since they're both 0
                if (promptCount == 0 and responseCount == 0):
                    # print("\n" + "DISCARD")
                    continue
                    print(chunk)

                # condition 2: split chunk
                elif (promptCount == responseCount):
                    # print( "\n" + "SPLIT SENTENCE, P: " + str(promptCount) + " R: " + str(responseCount))
                    print(chunk)

                # condition 3: classify as prompt
                elif (promptCount > responseCount):
                    # print( "\n" + "PROMPT, P: " + str(promptCount) + " R: " + str(responseCount))
                    
                    if not responseBuffer: # add to promp
                        promptBuffer += " " + chunk
                    else:
                        # trigger formatting
                        # format(promptBuffer, responseBuffer)
                        # clear promptBuffer and responseBuffer after writing
                        continue
                    print(chunk)

                # condition 4: classify as response
                elif (responseCount > promptCount):
                    # print( "\n" + "RESPONSE, P: " + str(promptCount) + " R: " + str(responseCount))
                    # skip until prompt is found
                    if not promptBuffer:
                        continue
                    # if there is prompt set conditional to RESPONSE
                    else:
                        responseBuffer += " " + chunk

                    print(chunk)


        # if match to prompt
        # then write to prompt

        # elif match to respnose
        # then write to prompt


        # close text file


""" 
Format into the form: 
{"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}
Into the file 
"""
def format(prompt, response):

    with open(outputDirectory / outFile, "w", encoding="UTF-8") as out:
        
        # write format example
        out.write('// {"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}')
        out.write('\n\n')

        out.write('{"messages":[{"role":"user","content":"' + prompt + '"},{"role":"assistant","content":"' + response + '"}]}')

if __name__ == "__main__":
    main()