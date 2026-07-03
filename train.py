from pathlib import Path
from datasets import load_dataset
import torch
from trl import SFTTrainer, SFTConfig
from peft import LoraConfig
from transformers import AutoModelForCausalLM, BitsAndBytesConfig

# import os
# os.environ["ACCELERATE_MIXED_PRECISION"] = "fp16"
# os.environ["TORCH_AUTOCAST_ALLOW_BF16"] = "0"

# Configure 4-bit Quantization
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16, #torch.bfloat16,
    bnb_4bit_use_double_quant=True,
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

data_set = load_dataset("json", data_files="TrainingSet/dataset.jsonl", split="train")
# # Configure training arguments
# training_args = SFTConfig(
#     packing=True,               # pack examples to improve training efficiency
#     assistant_only_loss=True,   # for conversational dataset
    
#     num_train_epochs=3,
#     learning_rate=2.0e-4,
#     max_seq_length=1024,
#     per_device_train_batch_size=1,
#     gradient_checkpointing=True,
# )
configurations = [
    # default config
    SFTConfig(
        learning_rate=2.0e-4,
        num_train_epochs=3,
        packing=True,               # pack examples to improve training efficiency
        assistant_only_loss=True,   # for conversational dataset
        max_length=1024,
        per_device_train_batch_size=1,
        gradient_checkpointing=False,
        seed=42,
        gradient_accumulation_steps=4,
        logging_steps=10,
        warmup_ratio=0.03,
        bf16=False,
        fp16=False,
        max_grad_norm=0.0,   # IMPORTANT: disables scaling path issues !!
        save_strategy="steps",
        save_steps=50,
        save_total_limit=2,
    ),
    # lower learning rate
    SFTConfig(
        learning_rate=1.0e-4,
        num_train_epochs=3,
        packing=True,
        assistant_only_loss=True,
        max_length=1024,
        per_device_train_batch_size=1,
        gradient_checkpointing=False,
        seed=42,
        gradient_accumulation_steps=4,
        logging_steps=10,
        warmup_ratio=0.03,
        bf16=False,
        fp16=False,
        max_grad_norm=0.0,   # IMPORTANT: disables scaling path issues
        save_strategy="steps",
        save_steps=50,
        save_total_limit=2,
    ),
    # more training
    SFTConfig(
        learning_rate=2.0e-4,
        num_train_epochs=5,
        packing=True,
        assistant_only_loss=True,
        max_length=1024,
        per_device_train_batch_size=1,
        gradient_checkpointing=False,
        seed=42,
        gradient_accumulation_steps=4,
        logging_steps=10,
        warmup_ratio=0.03,
        bf16=False,
        fp16=False,
        max_grad_norm=0.0,   # IMPORTANT: disables scaling path issues
        save_strategy="steps",        
        save_steps=50,
        save_total_limit=2,

    ),
    # larger effective batch
    SFTConfig(
        learning_rate=2.0e-4,
        num_train_epochs=3,
        gradient_accumulation_steps=8,
        packing=True,
        assistant_only_loss=True,
        max_length=1024,
        per_device_train_batch_size=1,
        gradient_checkpointing=False,
        seed=42,
        logging_steps=10,
        warmup_ratio=0.03,
        bf16=False,
        fp16=False,
        max_grad_norm=0.0,   # IMPORTANT: disables scaling path issues
        save_strategy="steps",
        save_steps=50,
        save_total_limit=2,

    ),
]

# print("Sanity Test")
# trainer.train()
# trainer.save_model("Adapters")

# training loop
for i, training_set in enumerate(configurations):

    output_dir = f"Checkpoints/config_{i}"

    # Load Model with Quantization
    model = AutoModelForCausalLM.from_pretrained(
        "Qwen/Qwen2.5-3B-Instruct", # checks local cache first
        quantization_config=bnb_config,
        device_map="auto",
        torch_dtype=torch.float16,
        attn_implementation="eager",
    )
    # model.config.torch_dtype = torch.float16

    trainer = SFTTrainer(
    model=model,
    train_dataset = data_set,
    peft_config=peft_config,
    args=training_set,
    )

    trainer.args.output_dir = output_dir
    checkpoints = sorted(Path(output_dir).glob("checkpoint-*"))

    if checkpoints:
        trainer.train(resume_from_checkpoint=str(checkpoints[-1]))
    else:
        trainer.train()

    trainer.save_model(f"Adapters/Set_{i}")