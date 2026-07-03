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

prompt = ("You are advising a real estate investment firm evaluating a diversified portfolio across residential "
    "and multifamily assets. Assume a regional banking crisis has tightened credit availability for commercial "
    "real estate lending, cap rates have expanded by 75–100 basis points in secondary markets, and national "
    "multifamily rent growth has plateaued. The portfolio includes urban core luxury apartments with high occupancy "
    "and high debt leverage, suburban workforce housing with moderate growth and stable occupancy, and newly built "
    "Class A developments in tertiary markets that are in the lease-up phase and exposed to refinancing risk. Write an "
    "investment risk and positioning memo that assesses how each asset class is affected under current conditions, "
    "identifies liquidity and refinancing risks across the portfolio, recommends whether to hold, divest, refinance, "
    "or reposition each segment, explains how tighter credit changes expected returns and exit timing assumptions, and "
    "concludes with a concise investment stance summarized in 3–5 bullet points. Use an institutional, precise, and "
    "explicitly analytical tone rather than generic financial advice."
    )

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