from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel
import torch


model_name="Qwen/Qwen2.5-3B-Instruct"

# load basic model, to be expanded upon
base_model = AutoModelForCausalLM.from_pretrained(
    model_name = model_name,
    torch_dtype=torch.float16,
    device_map="auto"
)

# load PEFT adapters
model = PeftModel.from_pretrained(base_model, "./Adapters")

model = model.merge_and_unload()

tokenizer = AutoTokenizer.from_pretrained(model_name)

prompt = "Give me a short introduction to large language model."
messages = [
    {"role": "system", "content": "You are Qwen, created by Alibaba Cloud. You are a helpful assistant."},
    {"role": "user", "content": prompt}
]
text = tokenizer.apply_chat_template(
    messages,
    tokenize=False,
    add_generation_prompt=True
)
model_inputs = tokenizer([text], return_tensors="pt").to(model.device)

generated_ids = model.generate(
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

response = tokenizer.batch_decode(generated_ids, skip_special_tokens=True)[0]

print(response)