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

# patterns to classify chunks of text
promptPattern = "|".join(re.escape(x) for x in promptKeys)
responsePattern = "|".join(re.escape(x) for x in responseKeys)
discardPattern = "|".join(re.escape(x) for x in discardKeys)

def main():
    clearOutput()
    classify()
    # format("MJ's catch phrase?", "HEEEEE HEEEEEE`!")

""" 
Potential functionality to add: Classfy more distinctly (e.g. by market data, context, analyst commentary, investment outlook / prediction)
!!!Combine consecutive prompts or consecutive responses rather than dropping.!!!
"""
def classify():

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
                    print("\n" + "DISCARD")
                    print(chunk)

                # condition 2: PROMPT
                elif (promptCount > responseCount):
                    print( "\n" + "PROMPT, P: " + str(promptCount) + " R: " + str(responseCount))
                    
                    # trigger formatting and clear buffers
                    if responseBuffer:
                        format(promptBuffer, responseBuffer)
                        promptBuffer = ""
                        responseBuffer = ""
                    
                    # add to promp
                    promptBuffer += " " + chunk
                    print(chunk)

                # condition 3: RESPONSE
                elif (responseCount > promptCount):
                    print( "\n" + "RESPONSE, P: " + str(promptCount) + " R: " + str(responseCount))
                    
                    # skip until prompt is found
                    if promptBuffer:
                        responseBuffer += " " + chunk

                    print(chunk)

                # condition 4: SPLIT (promptCount == responseCount)
                else: # (
                    print( "\n" + "SPLIT SENTENCE, P: " + str(promptCount) + " R: " + str(responseCount))
                    print(chunk)
                    # format and clear
                    if responseBuffer:
                        format(promptBuffer, responseBuffer)
                        promptBuffer = ""
                        responseBuffer = ""
                    
                    # with / without prompt, without responseBuffer
                    promptSplit, responseSplit = split(promptBuffer + " " + chunk)
                    if promptSplit:
                        # promptBuffer += " " + promptSplit
                        promptBuffer = promptSplit
                        # print("\nPrompt: " + promptBuffer)
                    if responseSplit:
                        # responseBuffer += " " + responseSplit
                        responseBuffer = responseSplit
                        # print("\nResponse: " + responseBuffer)
                    # print(chunk)

            # flush buffer before returning; "with open" closes file automatically
            if (promptBuffer or responseBuffer):
                format(promptBuffer, responseBuffer)

    
""" 
Take a chunk of text, split it (by units of sentence) into a prompt-response pair.
Invariant:  Split must always return a prompt. 
            Not doing so will break classify() logic i.e. responseBuffer cannot be filled before promptBuffer
Heuristic:  1. Split into sentences. First goes to promptSplit. 
            2. Add sentences to promptSplit until ...
            3. A sentence with responseKeywords is found. Rest of sentences go to resonseSplit
"""
def split(chunk):

    promptSplit = ""
    responseSplit = ""

    # split chunk into sentences, assign first to prompt
    # sentences = re.split(r'(?<=[.!?])\s+', chunk)
    sentences = re.split(r'(?<=[.?!])\s+(?=[A-Z])', chunk)

    # begin from 2nd sentence
    # splitIndex = 1
    splitIndex = len(sentences)
    for i, s in enumerate(sentences[1:], start=1):
        if (re.search(responsePattern, s, re.IGNORECASE)):
            splitIndex = i
            break

    promptSplit = " ".join(sentences[:splitIndex])
    responseSplit = " ".join(sentences[splitIndex:])
    print("LOOK HERE: " + str(sentences) + "Split Index: " + str(splitIndex))
    return promptSplit, responseSplit

""" 
Format into the form: 
{"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}

Writes into the file train.jsonl
"""
def format(prompt, response):

    with open(outputDirectory / outFile, "a", encoding="UTF-8") as out:
        # out.write('{"messages":[{"role":"user","content":"' + prompt + '"},{"role":"assistant","content":"' + response + '"}]}')
        out.write("Prompt:\n" + prompt + "\n" + "Response:\n" + response + "\n\n")

"""
Clear train.jsonl
"""
def clearOutput():
    with open(outputDirectory / outFile, "w", encoding="UTF-8") as out:
        # write format example
        out.write('// {"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}')
        out.write('\n\n')
        pass

if __name__ == "__main__":
    main()