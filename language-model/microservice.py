import os
from io import BytesIO
import time
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from threading import Thread
from transformers import AutoModelForCausalLM, AutoTokenizer, TextIteratorStreamer
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
        def token_generator():
            for word in response.split(" "):
                yield word + " "
                time.sleep(0.05)

    else:
        # Tokenize Prompt
        text = tokenizer.apply_chat_template(
            # messages,
            req.messages,
            tokenize=False,
            add_generation_prompt=True
        )
        model_inputs = tokenizer([text], return_tensors="pt").to(model.device)

        # token streamer
        streamer = TextIteratorStreamer(
            tokenizer,
            skip_prompt=True,
            skip_special_tokens=True,
        )

        # call LLM on a thread to prevent blocking
        thread = Thread(
            target=model.generate,
            kwargs=dict(
                **model_inputs,
                max_new_tokens= 512,
                temperature= 0.7,
                top_p= 0.9,
                do_sample= True,
                streamer= streamer,
            )
        )

        thread.start()

        def token_generator():
            for token in streamer:
                yield token
            thread.join()

    return StreamingResponse(token_generator(), media_type="text/plain")

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
    
    suffix = Path(file.filename).suffix.lower()
    doc = None
    text = None

    if suffix == ".pdf":
        doc = pymupdf.open(stream=contents, filetype="pdf")
        doc = "\n".join(page.get_text() for page in doc)
        # handle restructuring
    elif suffix == ".docx":
        doc = Document(BytesIO(contents))
        doc = "\n".join(p.text for p in doc.paragraphs)
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