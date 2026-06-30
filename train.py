from datasets import load_dataset
import torch
from trl import SFTTrainer, SFTConfig
from peft import LoraConfig
from transformers import AutoModelForCausalLM, BitsAndBytesConfig


# Configure 4-bit Quantization
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16, #torch.bfloat16,
    bnb_4bit_use_double_quant=True,
)

# Load Model with Quantization
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-3B-Instruct", # checks local cache first
    quantization_config=bnb_config,
    device_map="auto",
)

# Congifure LoRA, LoraConfig -> tells peft what adapters to change
peft_config = LoraConfig(
    r=16,                   # LoRA Rank, 8-16 typical
    lora_alpha=32,          # LoRA scaling factor, r * 2 so 32->64 or 16->32 for pairing consistenct
    lora_dropout=0.05,      # Dropout Probability
    bias="none",            # Bias Training Strategy
    task_type="CAUSAL_LM",  # Task type
    target_modules=["q_proj", "v_proj"],#, "k_proj", "o_proj"], # Modules to apply LoRA
    modules_to_save=None,   # Additinoal Modules to train
)

# Configure training arguments
training_args = SFTConfig(
    packing=True,               # pack examples to improve training efficiency
    assistant_only_loss=True,   # for conversational dataset
    
    num_train_epochs=3,
    learning_rate=2.0e-4,
    max_seq_length=1024,
    per_device_train_batch_size=1,
    gradient_checkpointing=True,
)

# SFTTrainer -> runs supervised fine-tuning
trainer = SFTTrainer(
    model=model,
    train_dataset = load_dataset("json", data_files="TrainingSet/dataset.jsonl", split="train"),
    peft_config=peft_config,
    args=training_args,
)

print("Sanity Test")
# trainer.train()
# trainer.save_model("A dapters")