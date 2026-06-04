# LLM Fine-Tuning Project
I want an AI assistent fine-tuned for real estate property analysis.
Such a tool should be a useful aid in providing guidance for assessing
residential assets such as single family and smaller multi-family 
properties for investment purposes.

## Stages

### 1. Data Parsing

### 2. Data Grouping

### 3. Training Parameter Adjustment

### 4. Fine-Tuning

### 5. Generation Parameter Adjustment

### 6. Repeat 3-5 Until Inferences Are Satisfactory

## How To Use / Naviagate / Details
I recommend setting up a Conda environment as I have:
 - Python 3.11.15
 - PyMuPDF
 - ...

Training a Qwen 2.5 3B with 1.5 million tokens at 2-3 epochs.
QLoRA training.
This is the most I can do with my hardware. 
/Reports directory contains pdfs with market reports or similar documents
/Raw directory contains sequentially named .txt files parallel to the /Reports