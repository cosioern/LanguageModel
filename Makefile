PYTHON = python3.11

main:
	$(PYTHON) parser.py
	$(PYTHON) classify.py

infer:
	$(PYTHON) inference.py

train:
	$(PYTHON) train.py