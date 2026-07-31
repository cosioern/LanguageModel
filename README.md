# Real Estate AI Assistant
PIE, what I've named the assistant, is a fine-tuned langauage modely deployed as a web application. PIE is simultaneously a proof-of-concept and a learning exercise to primarily explore AI development / deployment, and secondarily full-stack application development. Such a tool ought be a useful aid in providing guidance for assessing residential assets or the state of the market.  
This project is in continuous development. Currently, direct user-interactive features are the main focus, thought it will soon return to the refinment of the AI through further training rounds, tuning generation parameters, and honing the RAG pipeline.

## Getting Started
### Set Up a Conda Environment for the LM pipeline
I recommend setting up a Conda environment like so:
 - On Windows / MacOS: conda create -n "Environment Name" python=3.11
 - pip install pymupdf transformers datasets peft trl accelerate bitsandbytes
 - pip install torch --index-url https://download.pytorch.org/whl/cu121
 - (no longer recommend) ~~pip install --upgrade torch --index-url https://download.pytorch.org/whl/cu128~~
 - pip install fastapi uvicorn python-docx llama-index sentence-transformers python-multipart  

Python 3.11 version is the newest that will most reliably work with the necessary dependencies. This torch build is geared towards allowing PyTorch to make use of CUDA cores on NVidia GPUs, tried on RTX3060ti. You're mileage will vary with different hardware.

Database:
 - Postgres v18 install does not come with vector binaries, install from this [Repo](https://github.com/andreiramani/pgvector_pgsql_windows)
 - To reset db: DROP SCHEMA CASCADE; CREATE SCHEMA public AUTHORIZATION [USER]; GRANT ALL ON SCHEMA public to [USER]; CREATE EXTENSION IF NOT EXISTS vector;

Spring Boot don't require any special setup.

For React:
 - npm install react-router-dom

Set the Enviornment Variables:
   - MOCK_MODE       (microservice.py)    determines if LM is used to generate responses
   - JWT_KEY         (application.yml)    generate by runing GenerateKey.java
   - EMAIL           (application.yml)    email to use to send verification links
   - SMTP_PASSWORD   (application.yml)    app password to use smtp (with email)
   - DB_USER         (application.yml)    your username for Postgres

Download the dataset, link at #5 below, into the /TraningSet folder.

## Dataset Building & Training Pipeline:
### 1. Sourcing Reports
The bulk of documents used to generate this dataset were yearly reports covering several corners of the market such as broader economic markers, residential, multifamily, industrial, retail, capital markerts, and data centers.
To cite where these documents were sourced: CBRE, JLL, Colliers, US HUD, Freddie Mac, Deloitte, JCHS, and JP Morgan.
### 2. Data Parsing
parser.py scrapes text from pdf documents, joins text of close proximity, removes any chunks that are less than a given length, and writes the resulting text file into /Raw  
Reports used in dataset generation won't be found in this repo, find the dataset at #4
### 3. Data Grouping
classifier.py takes aforementioned text files, parses them, and classifies the resulting chunks of text as either a prompt, response, or to be discarded based on the presence of certain key words/phrases.
### 4. Final filter
filter.py continues the work done by parser.py and classify.py buy continuing to reduce noise, removing incomplete example pairs, removing examples with broken unicode characeters (that might break the trainer), and printing extracted examples for review
### 4. Training Parameter Set-Up
train.py contains fours sets of SFTConfig parameters used to train the LoRA adapters, each set saving its progress in a folder /set_i within /Adapters
### 5. Fine-Tuning and Evaluation
Find the training set on my HuggingFace page: [Dataset](https://huggingface.co/datasets/cosioe/RealEstate.v2).
train.py contains four SFTConfig (training parameters), outputs checkpoints to /Checkpoints_config_{i}, and Adapters to /Adapters/Set_{i}  
You may need to play around with these parameters to fit to your hardware. For example, the torch_dtype should be able to handle bfloats, but information is sparse, and attempting them on my own hardware led to errors. Other parameters may be adjusted to prevent crashes or improve strength of results dependeing on your hardware.

First Round of Training:
 - Began with four sets of SFTConfig() parameter configurations. Using four prompts and the same generation parameters,
their generations were compared by Claude as an LM-as-a-judge.
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
Of the second round of training, only 3 succeeded without crashes / corruption. Current frontrunner in use.

### 6. Adjusting Generation Parameters
In progress.

### 7. Building Out Webserver
This chatbot follows the simple formula shared by ChatGPT or Claude.
   - A FastAPI microservice handles LM generations and prompt/document embeddings
   - A Postgres DB persists User/Guest details, access tokens, chat history, and document embeddings.
   - Spring Boot serves LM generations, chat history, persistence, session tokens, verificatoin emails, etc via endpoints
   - A React (HTML/CSS/JS) frontend serves webpages like a registration, account, and chat pages.


## Note! Create the Following Folders:
/Reports        to hold pdfs of market reports or analyst commentary  
/Raw directory  to hold files of scraped text corresponding to /Reports  
/TrainingSet    to hold formatted jsonl file used in training  
/Adapters       to hold the LoRA adapter weights after training, to be used in inference generation  
