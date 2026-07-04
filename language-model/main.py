from fastapi import FastAPI
from pydantic import BaseModel

from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch


# load
app = FastAPI()

# Expects a JSON object with field "promopt"
class GenerateRequest(BaseModel):
    prompt: str


# Load Module
base_model = AutoModelForCausalLM.from_pretrained(
    # model_name = "Qwen/Qwen2.5-3B-Instruct",
    "Qwen/Qwen2.5-3B-Instruct",
    torch_dtype=torch.float16,
    device_map="auto",
)
# model = PeftModel.from_pretrained(base_model, "./Adapters")
# model = model.merge_and_unload()
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-3B-Instruct")


# Expose REST Endpoint, Receive the Prompt as JSON
@app.post("/generate")
def generate(req: GenerateRequest):

    messages = [
    {"role": "system", "content": "You are Qwen, created by Alibaba Cloud. You are a helpful assistant."},
    {"role": "user", "content": req.prompt}
    ]

    # Tokenize Prompt
    text = tokenizer.apply_chat_template(
    messages,
    tokenize=False,
    add_generation_prompt=True
    )
    model_inputs = tokenizer([text], return_tensors="pt").to(base_model.device)

    # Run Generator
    generated_ids = base_model.generate(
    **model_inputs,
    max_new_tokens=128, #512
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
