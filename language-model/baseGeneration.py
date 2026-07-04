from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch
from pathlib import Path

model_name="Qwen/Qwen2.5-3B-Instruct"
adapter_dir = Path("Adapters")

# load basic model, to be expanded upon
base_model = AutoModelForCausalLM.from_pretrained(
    pretrained_model_name_or_path = model_name,
    torch_dtype=torch.float16,
    device_map="auto",
)

tokenizer = AutoTokenizer.from_pretrained(model_name)

prompt = ("What's the current state of office space demand, including how remote/hybrid work trends are shaping vacancy rates"
                     "and landlord strategy?")

system_prompt = (
        "You are PIE, a real estate market analyst. "
        "Provide concise, investment-focused commentary. "
        "Base reasoning on supply, demand, interest rates, demographics, and valuation. "
        "Avoid speculation and avoid making up specific local statistics. "
        "Prioritize causal explanations and investment implications."
)

messages = [
    {"role": "system", "content": system_prompt},
    {"role": "user", "content": prompt}
]

text = tokenizer.apply_chat_template(
    messages,
    tokenize=False,
    add_generation_prompt=True
)

model_inputs = tokenizer([text], return_tensors="pt").to(base_model.device)

generated_ids = base_model.generate(
**model_inputs,
max_new_tokens=256, #128
# temperature=0.7,
# top_p=0.9,
# # top_k=20,
# # repetition_penalty=1.1,
# do_sample=True,
)

generated_ids = [
    output_ids[len(input_ids):] for input_ids, output_ids in zip(model_inputs.input_ids, generated_ids)
]

response = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0] 

print(response)