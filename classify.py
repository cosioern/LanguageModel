from pathlib import Path
import re

inputDirectory = Path("Raw")
outputDirectory = Path("TrainingSet")
outFile = "dataset.jsonl"

""" Property Value = NOI / CR ~ if cap rate falls an NOI remains same, property values increase.
    Good for current holders (can sell higher). Current buyers are betting on future growth.
"""
# better to have fewer, more specifically relevant keywords + lower count barrier
promptKeys =    ["market conditions", "inflation", "supply and demand", "vacancy", "net absorption", 
                "market share", "net absorption", "consecutive quarters", "gross domestic product",
                "GDP", "payroll expansion", "economic impact", "unemployment rate", "home sales",
                "median sales price", "year-over-year", "population growth", "declined to",
                "slowed", "net in-migration", "net out-migration", "natural change", "increased to", 
                "accounted for", "delinquent mortgages", "real estate owned", "REO", "peaked at", 
                "on record since", "According to", "in the past", "ranging from", "during that period",
                "at a peak of", "increased an average of", "in the years that followed", "during the next",
                "HOI", "declined from", "increased from", "historical average", "during the housing crisis",
                "annual rate of", "rental units permitted", "began to recover", "increased through", "cost burden",
                "recovery", "job growth", "job gains", "job losses", "job losses", r"from\s+\d{4}\s+through", 
                "continued to", "have expanded", "during the past", "prices averaged", "each year since",
                "net household growth", "submarket", "currently comprise", 
                ]
responseKeys = ["real estate", "investment activity", "cap rates", "leasing activity", 
                "leasing volume", "depreciate", "we expect", "will likely", "expected to", 
                "fundamentals", "supply overhang", "will continue", "we remain", "is expected",
                "should continue", "forcasts", "is estimated for", "will meet",
                "under construction will", "forecast period", "are expected", "will increase",
                "will decrease", "is anticipated", "anticipated to", "fastest rate of growth",

                ]
discardKeys =   ["copyright", "all rights reserved", "disclaimer", "disclaims all liability",
                "waive all claims", "figure", "©", "trademarks", "Intelligent Investment",
                "Fannie Mae", "Multifamily Market Commentary", "Table of contents",
                "C h i c a g o - N a p e r v i l l e ", 
                "C O M P R E H E N S I V E H O U S I N G M A R K E T A N A L Y S I S",
                "COMPREHENSIVE HOUSING MARKET ANALYSIS",
                "M a d i s o n , W I ",
                "M i l w a u k e e - Wa u k e s h a -",
                "Department of Housing and Urban Development",
                "Multifamily Economic"
                ]

# patterns to classify chunks of text
promptPattern = "|".join(re.escape(x) for x in promptKeys)
responsePattern = "|".join(re.escape(x) for x in responseKeys)
discardPattern = "|".join(re.escape(x) for x in discardKeys)

def main():
    clearOutput()
    classify()

""" 
Potential functionality to add: Classfy more distinctly (e.g. by market data, context, analyst commentary, investment outlook / prediction)
"""
def classify():

    # cycle through /Raw
    for item in inputDirectory.iterdir():

        # skip directories, empty files, and hidden files
        if not item.is_file() or item.stat().st_size == 0 or item.name.startswith("."):
            continue
        
        promptBuffer = ""
        responseBuffer = ""

        with open(item, "r", encoding="UTF-8") as raw:
            # split chunks of text into an array by \n, ignoring whitespace
            chunks = [c for c in re.split(r'\n{1,3}', raw.read()) if c.strip()]

            # if classification is same as before (i.e. two prompts in a row) drop chunk
            for chunk in chunks:

                # section headers / chart data (noise)
                if (re.search(r'[\w.-]+@[\w.-]+', chunk, re.IGNORECASE)
                or not (re.search(r'[.!?](\s|$)', chunk, re.IGNORECASE))):
                    continue

                # remove sentences with discardKeys
                sentences = re.split(r'(?<=[.?!])\s+', chunk)
                sentences = [s for s in sentences if not re.search(discardPattern, s, re.IGNORECASE)]
                chunk = " ".join(sentences)

                promptCount = len(re.findall(promptPattern, chunk, re.IGNORECASE))
                responseCount = len(re.findall(responsePattern, chunk, re.IGNORECASE))

                # condition 1: discard
                if (promptCount == 0 and responseCount == 0):
                    continue

                # condition 2: PROMPT
                elif (promptCount > responseCount):
                    # trigger formatting and clear buffers
                    if responseBuffer:
                        format(promptBuffer, responseBuffer)
                        promptBuffer = ""
                        responseBuffer = ""
                    
                    # add to promp
                    promptBuffer += " " + chunk

                # condition 3: RESPONSE
                elif (responseCount > promptCount):                    
                    # skip until prompt is found
                    if promptBuffer:
                        responseBuffer += " " + chunk

                # condition 4: SPLIT (promptCount == responseCount)
                else: # (
                    # format and clear
                    if responseBuffer:
                        format(promptBuffer, responseBuffer)
                        promptBuffer = ""
                        responseBuffer = ""
                    
                    # with / without prompt, without responseBuffer
                    promptSplit, responseSplit = split(promptBuffer + " " + chunk)
                    if promptSplit:
                        promptBuffer = promptSplit

                    if responseSplit:
                        responseBuffer = responseSplit

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

    # first sentence will be withheld to ensure a prompt (can't have a response w/out prompt)
    # in the case that no responseKeys are found, entire chunk remains prompt
    splitIndex = len(sentences)
    for i, s in enumerate(sentences[1:], start=1):
        if (re.search(responsePattern, s, re.IGNORECASE)):
            splitIndex = i
            break

    promptSplit = " ".join(sentences[:splitIndex])
    responseSplit = " ".join(sentences[splitIndex:])

    return promptSplit, responseSplit

""" 
Format into the form: 
{"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}

Writes into the file train.jsonl
"""
def format(prompt, response):

    with open(outputDirectory / outFile, "a", encoding="UTF-8") as out:
        out.write('\n{"messages":[{"role":"user","content":"' + prompt + '"},{"role":"assistant","content":"' + response + '"}]}')
        # out.write("Prompt:\n" + prompt + "\n" + "Response:\n" + response + "\n\n")

"""
Clear train.jsonl
"""
def clearOutput():
    with open(outputDirectory / outFile, "w", encoding="UTF-8") as out:
        # write format example
        # out.write('// {"messages":[{"role":"user","content":"Prompt"},{"role":"assistant","content":"Response"}]}\n')
        pass

if __name__ == "__main__":
    main()