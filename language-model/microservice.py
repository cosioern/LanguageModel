import os
from io import BytesIO
from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel

from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch

import pymupdf
from pathlib import Path
from docx import Document
from llama_index.core.node_parser import SentenceSplitter
from sentence_transformers import SentenceTransformer

# Deserializes from: {"role":"...", "content":"..."}
class Message(BaseModel):
    role: str
    content: str
# Deserializes from: {"messages": [{}"role":"...", "content":"..."}, ...]}
class ChatRequest(BaseModel):
    messages: list[Message]
# Deserializes from: {"embedding":[x, y, z], "chunk":"..."}
class EmbeddedChunk(BaseModel):
    embedding: list[float]
    content: str

# load
app = FastAPI()
MOCK_MODE = os.getenv("MOCK_MODE", "false").lower() == "true"

# load module if not in mock mode (for laptop development)
model = None
if not MOCK_MODE:
    # Load Module
    base_model = AutoModelForCausalLM.from_pretrained(
        "Qwen/Qwen2.5-3B-Instruct",
        torch_dtype=torch.float16,
        device_map="auto",
    )

    # laod adapters, merge model for efficiency, and load tokenizer
    model = PeftModel.from_pretrained(base_model, "./Adapters2/Set_0")
    model = model.merge_and_unload()
    tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-3B-Instruct")

    # load model for chunk encoding
    encoder_model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
    # system_prompt = (
    #         "You are PIE, a real estate market analyst. "
    #         "Provide concise, investment-focused commentary. "
    #         "Base reasoning on supply, demand, interest rates, demographics, and valuation. "
    #         "Avoid speculation and avoid making up specific local statistics. "
    #         "Prioritize causal explanations and investment implications."
    # )

# Expose REST Endpoint, Receive the Prompt as JSON
"""
Endpoint handling the generation and return of a response of a fine-tuned LLM.
Arguments:
    ChatRequest: a list of {"role" : "content"} pairs used as the promopt to the LLM 
"""
@app.post("/generate")
def generate(req: ChatRequest):
    if MOCK_MODE:
        response = "This is a mock response for the prompt: " + req.messages[-1].content

    else:
        # messages = [
        # {"role": "system", "content": system_prompt},
        # {"role": "user", "content": req.prompt}
        # ]
        # messages = r.messages

        # Tokenize Prompt
        text = tokenizer.apply_chat_template(
        # messages,
        req.messages,
        tokenize=False,
        add_generation_prompt=True
        )
        model_inputs = tokenizer([text], return_tensors="pt").to(model.device)

        # Run Generator
        generated_ids = model.generate(
        **model_inputs,
        max_new_tokens=512,
        temperature=0.7,
        top_p=0.9,
        # top_k=20,
        # repetition_penalty=1.1,
        do_sample=True,
        )
        generated_ids = [
            output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
        ]
        # Decode Output
        response = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]

    # Return Generation as JSON
    return {"generation":response}

"""
Endpoint handling document embedding
Argument:
    file containg contents of document to be embedded
Return:
    Set of embeddings
"""
@app.post("/embedDocument")
async def embeddings(file: UploadFile = File(...)) -> list[EmbeddedChunk]:
    contents = await file.read()

    # if not file.filename.endswith((".pdf", ".txt", ".docx")):
    #     raise HTTPException(status_code=400, detail="Unsupported Data Type")
    
    suffix = Path(file.filename).suffix.lower()
    doc = None
    text = None

    if suffix == ".pdf":
        doc = pymupdf.open(stream=contents, filetype="pdf")
        # handle restructuring
    elif suffix == ".docx":
        doc = Document(BytesIO(contents))
        # handle restructuring
    elif suffix == ".txt":
        doc = contents.decode("utf-8")
        # probably don't need to do much restructuring
    else:
        raise HTTPException(status_code=400, detail="Unsupported File Type")

    # assume that doc is left as text after the restructuring above

    splitter = SentenceSplitter(
        chunk_size=512,
        chunk_overlap=50,
    )

    chunks = splitter.split_text(doc)
    embeddings = encoder_model.encode(chunks).tolist()

    return [EmbeddedChunk(content=c, embedding=e) for c, e in zip(chunks, embeddings)]

"""
Endpoint handling prompt-processing to be used by server
to conduct a similarity search on the embeddings in persistence.

Arguments:
    Message: a JSON DTO holding {"role" : "content"}

Returns: An embedding of the prompt as a fload[]
"""
@app.post("/embedPrompt")
def embedPrompt(req: Message) -> list[float]:
    return encoder_model.encode(req.content).tolist()