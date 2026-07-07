# LLM Fine-Tuning Project
I want an AI assistent fine-tuned for real estate property analysis.</n>
Such a tool should be a useful aid in providing guidance for assessing
residential assets such as single family and smaller multi-family 
properties for investment purposes.

### Conetending Names
 - AssetPilot
 - EstatePilot
 - EstateIQ
 - Prospect (like the gold prospectors)
 - Property Insight Engine (PIE)
 - Estate Intelligence Engine (EIE)

### Real Estate Report Sources:
 * CBRE
 * JLL
 * Colliers
 * US HUD
 * Freddie Mac
 * Deloitte
 * JCHS
 * JP Morgan

 ### Current Training Dataset Size: 2.3 million characters

## Repo Structure
### /Backend         holds server
### /language-model  holds LM microservice and all scripts for constructing dataset and fine-tunining

## Production Phases:

### 1. Data Parsing
parser.py scrapes text from pdf documents, joins text of close proximity, removes anything chunks that</n>
are less than a given length, and writes the resulting text file into /Raw
Reports used in dataset generation won't be found in this repo, access the dataset below at #4
### 2. Data Grouping
classifier.py takes aforementioned text files, parses them, and classifies the resultant chunks of text</n>
as either a prompt, response, or to be discarded based on the presence of certain key words/phrases.

### 3. Training Parameter Set-Up
train.py contains fours sets of SFTConfig parameters used to train the LoRA adapters, each set saving</n>
its progress in a folder /set_i within /Adapters

### 4. Fine-Tuning and Evaluation
Find the training set on my HuggingFace page: {add link}

First Round of Training:
 - Began with four sets of SFTConfig() parameter configurations. Using four prompts and the same generation parameters,
their generations were compared by Claude as an LLM-as-a-judge.
...both dynamically and through metrics such as loss or similarity embeddings to determine
the best set of hyperparemeters.
 - Ranking: base >= 4 >= 2 > 1 > 3
 - The fine-tuned models saw greater depth in content depth for commentary tasks, but at the cost
    of temporal staleness.
 - The base model did better at framing responses currently, but did worse at causal reasoning.
Verdict:
 - Remove noise from dataset.
 - Reduce temporal stalness
 - Retrain configurations 1 & 3
Ways to Reduce Temporal Staleness:
 - scrub dates from dataset
 - OR rebalance by publication years producing a more even spread and removing emphasis on any one year
 - find more publications from current day - although only a stable fix for this year

### 5. Adjusting Generation Parameters

## Setting Up The Environment
I recommend setting up a Conda environment like so:
 - On Windows / MacOS: conda create -n "Env Name" python=3.11
 - pip install pymupdf transformers datasets pef trl accelerate bitsandbytes
 - pip install torch --index-url https://download.pytorch.org/whl/cu121
 - pip install --upgrade torch --index-url https://download.pytorch.org/whl/cu128
 - pip install fastapi
 - pip install uvicorn
 - This python version is the newest that will most reliably work with the necessary dependencies.
 - This torch build is geared towards allowing PyTorch to make use of CUDA cores on supported NVidia GPUs.

Training a Qwen 2.5 3B with 1.5 million tokens at 2-3 epochs.
QLoRA training.

## Create the Following Folders:
/Reports        to hold pdfs of market reports or analyst commentary</n>
/Raw directory  to hold files of scraped text corresponding to /Reports
/TrainingSet    to hold formatted jsonl file used in training
/Adapters       to hold the LoRA adapter weights after training, to be used in inference generation
